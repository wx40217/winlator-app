package com.winlator;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.winlator.container.Container;
import com.winlator.library.GameLibraryStore;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Locale;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.container.Shortcut;
import com.winlator.contentdialog.ContentDialog;
import com.winlator.contentdialog.CreateFolderDialog;
import com.winlator.contentdialog.ShortcutSettingsDialog;
import com.winlator.core.AppUtils;
import com.winlator.core.ArrayUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ShortcutsFragment extends BaseFileManagerFragment<Shortcut> {
    private final ExecutorService loader = Executors.newSingleThreadExecutor();
    private GameLibraryStore library;
    private List<Shortcut> folderItems = new ArrayList<>(), allGames = new ArrayList<>();
    private SearchView search;
    private Spinner containerPicker;
    private ChipGroup collectionPicker;
    private View emptyView, recentSection;
    private TextView emptyHint, count, section, breadcrumb;
    private RecyclerView recentList;
    private int selectedContainer, generation;
    private String query = "";
    private boolean favoritesOnly, initializing, launchInFlight, restorePosition = true;
    private boolean loading;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewStyle = "LIST".equals(preferences.getString("shortcuts_view_style", "GRID")) ? ViewStyle.LIST : ViewStyle.GRID;
        library = new GameLibraryStore(requireContext());
        query = preferences.getString("library_query", "");
        favoritesOnly = preferences.getBoolean("library_favorites", false);
        selectedContainer = preferences.getInt("library_container", 0);
        if (manager.getContainerById(selectedContainer) == null) selectedContainer = 0;
        Container container = manager.getContainerById(preferences.getInt("library_folder_container", 0));
        String savedFolder = preferences.getString("library_folder", "");
        if (container != null && !savedFolder.isEmpty()) {
            File folder = new File(savedFolder), desktop = new File(container.getUserDir(), "Desktop");
            try {
                if (folder.isDirectory() && folder.getCanonicalPath().startsWith(desktop.getCanonicalPath() + File.separator)) {
                    ArrayList<File> parents = new ArrayList<>();
                    for (File file = folder; file != null && !file.equals(desktop); file = file.getParentFile()) parents.add(file);
                    for (int i = parents.size()-1; i >= 0; i--) folderStack.push(new Shortcut(container, parents.get(i)));
                }
            } catch (IOException ignored) {}
        }
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state) {
        View root = inflater.inflate(R.layout.game_library_fragment, parent, false);
        recyclerView = root.findViewById(R.id.RecyclerView);
        emptyTextView = root.findViewById(R.id.TVEmptyText);
        pasteButton = root.findViewById(R.id.BTPaste);
        pasteButton.setOnClickListener(v -> pasteFiles());
        search = root.findViewById(R.id.LibrarySearch);
        TextView searchText = search.findViewById(androidx.appcompat.R.id.search_src_text);
        searchText.setTextColor(AppUtils.getThemeColor(requireContext(), R.attr.libraryText));
        searchText.setHintTextColor(AppUtils.getThemeColor(requireContext(), R.attr.librarySecondaryText));
        ImageView searchIcon = search.findViewById(androidx.appcompat.R.id.search_mag_icon);
        searchIcon.setColorFilter(AppUtils.getThemeColor(requireContext(), R.attr.librarySecondaryText));
        containerPicker = root.findViewById(R.id.LibraryContainer);
        collectionPicker = root.findViewById(R.id.LibraryCollection);
        emptyView = root.findViewById(R.id.LibraryEmpty);
        emptyHint = root.findViewById(R.id.LibraryEmptyHint);
        count = root.findViewById(R.id.LibraryCount);
        section = root.findViewById(R.id.LibrarySection);
        breadcrumb = root.findViewById(R.id.LibraryPath);
        recentSection = root.findViewById(R.id.LibraryRecent);
        recentList = root.findViewById(R.id.LibraryRecentList);
        recentList.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        initializing = true;
        search.setQuery(query, false);
        search.clearFocus();
        collectionPicker.check(favoritesOnly ? R.id.LibraryFavorites : R.id.LibraryAll);
        ArrayList<String> names = new ArrayList<>();
        names.add(getString(R.string.library_all_containers));
        int selection = 0;
        for (Container container : manager.getContainers()) {
            names.add(container.getName());
            if (container.id == selectedContainer) selection = names.size()-1;
        }
        containerPicker.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, names));
        containerPicker.setSelection(selection);
        containerPicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int next = position == 0 ? 0 : manager.getContainers().get(position-1).id;
                if (next == selectedContainer) return;
                selectedContainer = next;
                filterChanged();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String value) { search.clearFocus(); return true; }
            @Override public boolean onQueryTextChange(String value) { query = value; filterChanged(); return true; }
        });
        collectionPicker.setOnCheckedChangeListener((group, id) -> { favoritesOnly = id == R.id.LibraryFavorites; filterChanged(); });
        root.findViewById(R.id.LibraryEmptyAction).setOnClickListener(v -> {
            if (query.isEmpty() && selectedContainer == 0 && !favoritesOnly) ((MainActivity)requireActivity()).navigateTo(R.id.menu_item_containers);
            else {
                query = ""; selectedContainer = 0; favoritesOnly = false;
                search.setQuery("", false); containerPicker.setSelection(0); collectionPicker.check(R.id.LibraryAll);
                applyFilters();
            }
        });
        initializing = false;
        return root;
    }

    private void filterChanged() {
        if (initializing || recyclerView == null) return;
        restorePosition = false;
        applyFilters();
    }

    @Override
    public void refreshContent() {
        if (recyclerView == null || !isAdded()) return;
        while (!folderStack.isEmpty() && !folderStack.peek().file.isDirectory()) folderStack.pop();
        Shortcut selectedFolder = folderStack.isEmpty() ? null : folderStack.peek();
        updateTitle();
        final int request = ++generation;
        loading = true;
        emptyView.setVisibility(View.GONE);
        getView().findViewById(R.id.LibraryLoading).setVisibility(View.VISIBLE);
        final android.app.Activity activity = requireActivity();
        loader.execute(() -> {
            List<Shortcut> current = manager.loadShortcuts(selectedFolder);
            List<Shortcut> games = new ArrayList<>();
            Set<String> visited = new HashSet<>();
            for (Container container : manager.getContainers()) {
                File desktop = new File(container.getUserDir(), "Desktop");
                loadGames(new Shortcut(container, desktop), games, visited, desktop, 0);
            }
            activity.runOnUiThread(() -> {
                if (!isAdded() || recyclerView == null || request != generation) return;
                loading = false;
                getView().findViewById(R.id.LibraryLoading).setVisibility(View.GONE);
                folderItems = current; allGames = games; applyFilters();
            });
        });
    }

    private void loadGames(Shortcut folder, List<Shortcut> games, Set<String> visited, File desktop, int depth) {
        if (depth > 32 || Thread.currentThread().isInterrupted()) return;
        try {
            String canonical = folder.file.getCanonicalPath(), root = desktop.getCanonicalPath();
            if ((!canonical.equals(root) && !canonical.startsWith(root + File.separator)) || !visited.add(canonical)) return;
        } catch (IOException ignored) { return; }
        for (Shortcut item : manager.loadShortcuts(folder)) {
            if (item.file.isDirectory()) loadGames(item, games, visited, desktop, depth+1);
            else games.add(item);
        }
    }

    private void applyFilters() {
        if (recyclerView == null || getView() == null || loading) return;
        String needle = query.trim().toLowerCase(Locale.ROOT);
        List<Shortcut> source = favoritesOnly || !needle.isEmpty() || selectedContainer != 0 ? allGames : folderItems;
        List<Shortcut> items = new ArrayList<>();
        for (Shortcut item : source) {
            if (selectedContainer != 0 && item.container.id != selectedContainer) continue;
            if (favoritesOnly && !library.isFavorite(item.container.id, item.file)) continue;
            if (!item.name.toLowerCase(Locale.ROOT).contains(needle)) continue;
            items.add(item);
        }
        items.sort((a, b) -> {
            int foldersFirst = Boolean.compare(b.file.isDirectory(), a.file.isDirectory());
            return foldersFirst != 0 ? foldersFirst : a.name.compareToIgnoreCase(b.name);
        });
        if (viewStyleNeedsUpdate || recyclerView.getLayoutManager() == null) {
            int spans = Math.max(2, (int)(getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density / 180));
            recyclerView.setLayoutManager(viewStyle == ViewStyle.GRID ? new GridLayoutManager(requireContext(), spans) : new LinearLayoutManager(requireContext()));
            viewStyleNeedsUpdate = false;
        }
        recyclerView.setAdapter(new ShortcutsAdapter(items));
        count.setText(getString(R.string.library_count, items.size()));
        section.setText(favoritesOnly ? R.string.favorites : R.string.library_all_games);
        emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        boolean filters = !needle.isEmpty() || selectedContainer != 0;
        emptyTextView.setText(filters ? R.string.library_no_results : favoritesOnly ? R.string.library_no_favorites : R.string.library_empty);
        emptyHint.setText(filters ? R.string.library_no_results_hint : favoritesOnly ? R.string.library_no_favorites_hint : R.string.library_empty_hint);
        ((TextView)getView().findViewById(R.id.LibraryEmptyAction)).setText(filters || favoritesOnly ? R.string.library_clear_filters : R.string.containers);
        List<Shortcut> recent = new ArrayList<>();
        if (folderStack.isEmpty() && !favoritesOnly && !filters) {
            for (Shortcut game : allGames) if (library.lastLaunch(game.container.id, game.file) > 0) recent.add(game);
            recent.sort(Comparator.comparingLong((Shortcut game) -> library.lastLaunch(game.container.id, game.file)).reversed());
            if (recent.size() > 5) recent = new ArrayList<>(recent.subList(0, 5));
        }
        recentSection.setVisibility(recent.isEmpty() ? View.GONE : View.VISIBLE);
        recentList.setAdapter(new ShortcutsAdapter(recent, true));
        if (restorePosition && !items.isEmpty()) {
            int position = Math.min(Math.max(0, preferences.getInt("library_position", 0)), items.size()-1);
            ((LinearLayoutManager)recyclerView.getLayoutManager()).scrollToPositionWithOffset(position, preferences.getInt("library_offset", 0));
            restorePosition = false;
        }
    }

    private void updateTitle() {
        StringBuilder title = new StringBuilder(getString(R.string.game_library));
        for (Shortcut folder : folderStack) title.append(" / ").append(folder.name);
        breadcrumb.setText(title);
        breadcrumb.setVisibility(folderStack.isEmpty() ? View.GONE : View.VISIBLE);
        ActionBar bar = ((AppCompatActivity)requireActivity()).getSupportActionBar();
        bar.setTitle(R.string.game_library);
        bar.setHomeAsUpIndicator(folderStack.isEmpty() ? R.drawable.icon_action_bar_menu : R.drawable.icon_action_bar_back);
    }

    private void saveBrowseState() {
        if (recyclerView == null) return;
        LinearLayoutManager layout = (LinearLayoutManager)recyclerView.getLayoutManager();
        int position = layout == null ? 0 : Math.max(0, layout.findFirstVisibleItemPosition());
        View first = layout == null ? null : layout.findViewByPosition(position);
        Shortcut folder = folderStack.isEmpty() ? null : folderStack.peek();
        preferences.edit().putString("library_query", query).putBoolean("library_favorites", favoritesOnly)
            .putInt("library_container", selectedContainer).putInt("library_position", position)
            .putInt("library_offset", first == null ? 0 : first.getTop() - recyclerView.getPaddingTop())
            .putString("library_folder", folder == null ? "" : folder.file.getAbsolutePath())
            .putInt("library_folder_container", folder == null ? 0 : folder.container.id).commit();
    }

    @Override public void onPause() { saveBrowseState(); super.onPause(); }
    @Override public void onResume() {
        super.onResume(); launchInFlight = false;
        ViewStyle configured = "LIST".equals(preferences.getString("shortcuts_view_style", "GRID")) ? ViewStyle.LIST : ViewStyle.GRID;
        if (configured != viewStyle) { viewStyle = configured; viewStyleNeedsUpdate = true; applyFilters(); }
    }
    @Override public void onDestroyView() { saveBrowseState(); generation++; recyclerView = null; super.onDestroyView(); }
    @Override public void onDestroy() { loader.shutdownNow(); super.onDestroy(); }
    @Override public boolean onOptionsMenuClicked() {
        boolean handled = super.onOptionsMenuClicked();
        if (handled) updateTitle();
        return handled;
    }

    @Override protected void onFilePasted(File origin, File target, boolean moved) {
        if (!moved) return;
        int oldContainer = findContainer(origin), newContainer = findContainer(target);
        if (oldContainer > 0 && newContainer > 0) library.move(oldContainer, origin, newContainer, target);
    }
    private int findContainer(File file) {
        for (Container container : manager.getContainers()) {
            if (file.getAbsolutePath().startsWith(container.getRootDir().getAbsolutePath() + File.separator)) return container.id;
        }
        return 0;
    }
    public void onShortcutRenamed(Shortcut shortcut, File newFile) {
        library.move(shortcut.container.id, shortcut.file, shortcut.container.id, newFile);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.shortcuts_menu, menu);
        refreshViewStyleMenuItem(menu.findItem(R.id.menu_item_view_style));
    }

    private void createFolder() {
        clearClipboard();
        if (manager.getContainers().isEmpty()) return;
        CreateFolderDialog createFolderDialog = new CreateFolderDialog(manager);
        createFolderDialog.setOnCreateFolderListener((container, name) -> {
            File desktopDir = new File(container.getUserDir(), "Desktop");
            File parent = !folderStack.isEmpty() ? folderStack.peek().file : desktopDir;
            File file = new File(parent, name);
            if (file.isDirectory()) {
                AppUtils.showToast(getContext(), R.string.there_already_file_with_that_name);
            }
            else {
                file.mkdir();
                refreshContent();
            }
        });
        createFolderDialog.show();
    }

    @Override
    protected void pasteFiles() {
        if (folderStack.isEmpty()) {
            clearClipboard();
            AppUtils.showToast(getContext(), R.string.you_cannot_paste_files_here);
            return;
        }

        if (clipboard == null) return;
        clipboard.targetDir = folderStack.peek().file;
        super.pasteFiles();
    }

    private void instantiateClipboard(Shortcut shortcut, boolean cutMode) {
        clearClipboard();
        File[] files = {new File(shortcut.file.getParentFile(), shortcut.file.getName())};
        if (shortcut.file.isFile() && !shortcut.isLinkPath()) {
            File linkFile = shortcut.getLinkFile();
            files = ArrayUtils.concat(files, new File[]{new File(linkFile.getParentFile(), linkFile.getName())});
        }

        clipboard = new Clipboard(files, cutMode);
        pasteButton.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.menu_item_view_style) {
            setViewStyle(viewStyle == ViewStyle.GRID ? ViewStyle.LIST : ViewStyle.GRID);
            preferences.edit().putString("shortcuts_view_style", viewStyle.name()).apply();
            refreshViewStyleMenuItem(menuItem);
            return true;
        }
        else if (itemId == R.id.menu_item_new_folder) {
            createFolder();
            return true;
        }
        else return super.onOptionsItemSelected(menuItem);
    }

    @Override
    protected String getHomeTitle() {
        return getString(R.string.game_library);
    }

    private class ShortcutsAdapter extends RecyclerView.Adapter<ShortcutsAdapter.ViewHolder> {
        private final List<Shortcut> data;
        private final boolean recent;

        private class ViewHolder extends RecyclerView.ViewHolder {
            private final View launch;
            private final View favorite;
            private final ImageView menuButton;
            private final ImageView imageView;
            private final TextView title;
            private final TextView subtitle;

            private ViewHolder(View view) {
                super(view);
                this.imageView = view.findViewById(R.id.ImageView);
                this.title = view.findViewById(R.id.TVTitle);
                this.subtitle = view.findViewById(R.id.TVSubtitle);
                this.launch = view.findViewById(R.id.LibraryItem);
                this.favorite = view.findViewById(R.id.LibraryFavoriteMark);
                this.menuButton = view.findViewById(R.id.BTMenu);
            }
        }

        public ShortcutsAdapter(List<Shortcut> data) {
            this(data, false);
        }
        public ShortcutsAdapter(List<Shortcut> data, boolean recent) {
            this.data = data;
            this.recent = recent;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int resource = recent || viewStyle == ViewStyle.LIST ? R.layout.game_library_list_item : R.layout.game_library_grid_item;
            View view = LayoutInflater.from(parent.getContext()).inflate(resource, parent, false);
            if (recent) {
                view.setLayoutParams(new RecyclerView.LayoutParams((int)(300 * getResources().getDisplayMetrics().density), ViewGroup.LayoutParams.MATCH_PARENT));
                view.setBackgroundResource(R.drawable.library_tint);
            }
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final Shortcut item = data.get(position);

            if (item.icon == null) {
                int iconResId = item.file.isDirectory() ? R.drawable.container_folder : R.drawable.container_file_link;
                holder.imageView.setImageResource(iconResId);
            }
            else holder.imageView.setImageBitmap(item.icon);

            holder.title.setText(item.name);
            holder.subtitle.setText(item.container.getName());

            holder.favorite.setVisibility(library.isFavorite(item.container.id, item.file) ? View.VISIBLE : View.GONE);
            holder.launch.setContentDescription(getString(item.file.isDirectory() ? R.string.library_open_folder : R.string.library_launch, item.name));
            holder.launch.setOnClickListener((v) -> runFromShortcut(item));
            holder.menuButton.setContentDescription(getString(R.string.library_more, item.name));
            holder.menuButton.setOnClickListener((v) -> showListItemMenu(v, item));
        }

        @Override
        public final int getItemCount() {
            return data.size();
        }

        private void showListItemMenu(View anchorView, final Shortcut shortcut) {
            final Context context = getContext();
            PopupMenu listItemMenu = new PopupMenu(context, anchorView);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) listItemMenu.setForceShowIcon(true);

            listItemMenu.inflate(R.menu.file_manager_popup_menu);

            Menu menu = listItemMenu.getMenu();
            menu.findItem(R.id.menu_item_rename).setVisible(false);
            menu.findItem(R.id.menu_item_settings).setVisible(shortcut.file.isFile());
            menu.findItem(R.id.menu_item_add_favorite).setVisible(shortcut.file.isFile())
                .setTitle(library.isFavorite(shortcut.container.id, shortcut.file) ? R.string.library_remove_favorite : R.string.add_to_favorites);

            listItemMenu.setOnMenuItemClickListener((menuItem) -> {
                int itemId = menuItem.getItemId();
                switch (itemId) {
                    case R.id.menu_item_add_favorite:
                        library.toggleFavorite(shortcut.container.id, shortcut.file);
                        applyFilters();
                        break;
                    case R.id.menu_item_info:
                        String[] keys = {"screenSize", "graphicsDriver", "graphicsDriverConfig", "dxwrapper", "dxwrapperConfig", "audioDriver", "audioDriverConfig", "box64Preset", "envVars", "wincomponents"};
                        boolean overrides = false;
                        for (String key : keys) if (!shortcut.getExtra(key).isEmpty()) overrides = true;
                        Snackbar.make(requireView(), shortcut.container.getName() + " · " + getString(overrides ? R.string.library_overrides : R.string.library_inherited), Snackbar.LENGTH_LONG).show();
                        break;
                    case R.id.menu_item_settings:
                        clearClipboard();
                        (new ShortcutSettingsDialog(ShortcutsFragment.this, shortcut)).show();
                        break;
                    case R.id.menu_item_copy:
                    case R.id.menu_item_cut:
                        instantiateClipboard(shortcut, itemId == R.id.menu_item_cut);
                        break;
                    case R.id.menu_item_remove:
                        clearClipboard();
                        ContentDialog.confirm(context, R.string.do_you_want_to_remove_this_file, () -> {
                            shortcut.remove();
                            if (!shortcut.file.exists()) library.remove(shortcut.container.id, shortcut.file);
                            refreshContent();
                        });
                        break;
                }
                return true;
            });
            listItemMenu.show();
        }

        private void runFromShortcut(Shortcut shortcut) {
            AppCompatActivity activity = (AppCompatActivity)getActivity();
            if (!shortcut.file.exists() || manager.getContainerById(shortcut.container.id) == null) {
                Snackbar.make(requireView(), R.string.library_missing_shortcut, Snackbar.LENGTH_LONG).show();
                refreshContent();
                return;
            }

            if (shortcut.file.isDirectory()) {
                folderStack.push(shortcut);
                restorePosition = false;
                refreshContent();

                updateTitle();
            }
            else {
                if (launchInFlight) return;
                launchInFlight = true;
                saveBrowseState();
                Intent intent = new Intent(activity, XServerDisplayActivity.class);
                intent.putExtra("container_id", shortcut.container.id);
                intent.putExtra("shortcut_path", shortcut.file.getPath());
                try {
                    activity.startActivity(intent);
                    library.recordLaunch(shortcut.container.id, shortcut.file, System.currentTimeMillis());
                } catch (android.content.ActivityNotFoundException | SecurityException exception) {
                    launchInFlight = false;
                    Snackbar.make(requireView(), R.string.library_launch_failed, Snackbar.LENGTH_LONG).show();
                }
            }
        }
    }
}
