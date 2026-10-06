cmake_minimum_required(VERSION 3.22.1)

if(NOT DEFINED WINLATOR_APPLICATION_ID)
    message(FATAL_ERROR "Pass WINLATOR_APPLICATION_ID from the Android applicationId")
endif()
add_compile_definitions(WINLATOR_APPLICATION_ID="${WINLATOR_APPLICATION_ID}")

add_subdirectory(winlator)
add_subdirectory(vortekrenderer)
add_subdirectory(virglrenderer)
add_subdirectory(midihandler)
add_subdirectory(libadrenotools)
add_subdirectory(gladiorenderer)
