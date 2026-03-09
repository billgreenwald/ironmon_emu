# Install script for directory: /home/bill/ironmon_emu/mgba-android-memapi/upstream

# Set the install prefix
if(NOT DEFINED CMAKE_INSTALL_PREFIX)
  set(CMAKE_INSTALL_PREFIX "/usr/local")
endif()
string(REGEX REPLACE "/$" "" CMAKE_INSTALL_PREFIX "${CMAKE_INSTALL_PREFIX}")

# Set the install configuration name.
if(NOT DEFINED CMAKE_INSTALL_CONFIG_NAME)
  if(BUILD_TYPE)
    string(REGEX REPLACE "^[^A-Za-z0-9_]+" ""
           CMAKE_INSTALL_CONFIG_NAME "${BUILD_TYPE}")
  else()
    set(CMAKE_INSTALL_CONFIG_NAME "Debug")
  endif()
  message(STATUS "Install configuration: \"${CMAKE_INSTALL_CONFIG_NAME}\"")
endif()

# Set the component getting installed.
if(NOT CMAKE_INSTALL_COMPONENT)
  if(COMPONENT)
    message(STATUS "Install component: \"${COMPONENT}\"")
    set(CMAKE_INSTALL_COMPONENT "${COMPONENT}")
  else()
    set(CMAKE_INSTALL_COMPONENT)
  endif()
endif()

# Install shared libraries without execute permission?
if(NOT DEFINED CMAKE_INSTALL_SO_NO_EXE)
  set(CMAKE_INSTALL_SO_NO_EXE "1")
endif()

# Is this installation the result of a crosscompile?
if(NOT DEFINED CMAKE_CROSSCOMPILING)
  set(CMAKE_CROSSCOMPILING "FALSE")
endif()

# Set default install directory permissions.
if(NOT DEFINED CMAKE_OBJDUMP)
  set(CMAKE_OBJDUMP "/usr/bin/objdump")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "libmgba" OR NOT CMAKE_INSTALL_COMPONENT)
  foreach(file
      "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libmgba.so.0.11.0"
      "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libmgba.so.0.11"
      )
    if(EXISTS "${file}" AND
       NOT IS_SYMLINK "${file}")
      file(RPATH_CHECK
           FILE "${file}"
           RPATH "/usr/local/lib")
    endif()
  endforeach()
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib" TYPE SHARED_LIBRARY FILES
    "/home/bill/ironmon_emu/mgba-android-memapi/build-desktop/libmgba.so.0.11.0"
    "/home/bill/ironmon_emu/mgba-android-memapi/build-desktop/libmgba.so.0.11"
    )
  foreach(file
      "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libmgba.so.0.11.0"
      "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libmgba.so.0.11"
      )
    if(EXISTS "${file}" AND
       NOT IS_SYMLINK "${file}")
      file(RPATH_CHANGE
           FILE "${file}"
           OLD_RPATH "::::::::::::::"
           NEW_RPATH "/usr/local/lib")
      if(CMAKE_INSTALL_DO_STRIP)
        execute_process(COMMAND "/usr/bin/strip" "${file}")
      endif()
    endif()
  endforeach()
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "mgba-dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib" TYPE SHARED_LIBRARY FILES "/home/bill/ironmon_emu/mgba-android-memapi/build-desktop/libmgba.so")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "mgba" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/icons/hicolor/16x16/apps" TYPE FILE RENAME "io.mgba.mGBA.png" FILES "/home/bill/ironmon_emu/mgba-android-memapi/upstream/res/mgba-16.png")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "mgba" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/icons/hicolor/24x24/apps" TYPE FILE RENAME "io.mgba.mGBA.png" FILES "/home/bill/ironmon_emu/mgba-android-memapi/upstream/res/mgba-24.png")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "mgba" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/icons/hicolor/32x32/apps" TYPE FILE RENAME "io.mgba.mGBA.png" FILES "/home/bill/ironmon_emu/mgba-android-memapi/upstream/res/mgba-32.png")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "mgba" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/icons/hicolor/48x48/apps" TYPE FILE RENAME "io.mgba.mGBA.png" FILES "/home/bill/ironmon_emu/mgba-android-memapi/upstream/res/mgba-48.png")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "mgba" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/icons/hicolor/64x64/apps" TYPE FILE RENAME "io.mgba.mGBA.png" FILES "/home/bill/ironmon_emu/mgba-android-memapi/upstream/res/mgba-64.png")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "mgba" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/icons/hicolor/96x96/apps" TYPE FILE RENAME "io.mgba.mGBA.png" FILES "/home/bill/ironmon_emu/mgba-android-memapi/upstream/res/mgba-96.png")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "mgba" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/icons/hicolor/128x128/apps" TYPE FILE RENAME "io.mgba.mGBA.png" FILES "/home/bill/ironmon_emu/mgba-android-memapi/upstream/res/mgba-128.png")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "mgba" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/icons/hicolor/256x256/apps" TYPE FILE RENAME "io.mgba.mGBA.png" FILES "/home/bill/ironmon_emu/mgba-android-memapi/upstream/res/mgba-256.png")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "mgba" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/icons/hicolor/512x512/apps" TYPE FILE RENAME "io.mgba.mGBA.png" FILES "/home/bill/ironmon_emu/mgba-android-memapi/upstream/res/mgba-512.png")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "mgba-headless" OR NOT CMAKE_INSTALL_COMPONENT)
  if(EXISTS "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/bin/mgba-headless" AND
     NOT IS_SYMLINK "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/bin/mgba-headless")
    file(RPATH_CHECK
         FILE "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/bin/mgba-headless"
         RPATH "/usr/local/lib")
  endif()
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/bin" TYPE EXECUTABLE FILES "/home/bill/ironmon_emu/mgba-android-memapi/build-desktop/mgba-headless")
  if(EXISTS "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/bin/mgba-headless" AND
     NOT IS_SYMLINK "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/bin/mgba-headless")
    file(RPATH_CHANGE
         FILE "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/bin/mgba-headless"
         OLD_RPATH "/home/bill/ironmon_emu/mgba-android-memapi/build-desktop:"
         NEW_RPATH "/usr/local/lib")
    if(CMAKE_INSTALL_DO_STRIP)
      execute_process(COMMAND "/usr/bin/strip" "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/bin/mgba-headless")
    endif()
  endif()
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "mgba-headless" OR NOT CMAKE_INSTALL_COMPONENT)
  include("/home/bill/ironmon_emu/mgba-android-memapi/build-desktop/CMakeFiles/mgba-headless.dir/install-cxx-module-bmi-Debug.cmake" OPTIONAL)
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "mgba-dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include" TYPE DIRECTORY FILES "/home/bill/ironmon_emu/mgba-android-memapi/upstream/include/mgba" FILES_MATCHING REGEX "/[^/]*\\.h$")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "mgba-dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include" TYPE DIRECTORY FILES "/home/bill/ironmon_emu/mgba-android-memapi/upstream/include/mgba-util" FILES_MATCHING REGEX "/[^/]*\\.h$")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "mgba-dev" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/mgba" TYPE FILE FILES "/home/bill/ironmon_emu/mgba-android-memapi/build-desktop/include/mgba/flags.h")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "mgba" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/doc/mGBA/licenses" TYPE FILE FILES "/home/bill/ironmon_emu/mgba-android-memapi/upstream/res/licenses/inih.txt")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "mgba" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/doc/mGBA/licenses" TYPE FILE FILES "/home/bill/ironmon_emu/mgba-android-memapi/upstream/res/licenses/discord-rpc.txt")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "mgba" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/doc/mGBA/licenses" TYPE FILE FILES "/home/bill/ironmon_emu/mgba-android-memapi/upstream/res/licenses/rapidjson.txt")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "mgba" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/doc/mGBA" TYPE FILE FILES
    "/home/bill/ironmon_emu/mgba-android-memapi/upstream/README.md"
    "/home/bill/ironmon_emu/mgba-android-memapi/upstream/README_DE.md"
    "/home/bill/ironmon_emu/mgba-android-memapi/upstream/README_ES.md"
    "/home/bill/ironmon_emu/mgba-android-memapi/upstream/README_JP.md"
    "/home/bill/ironmon_emu/mgba-android-memapi/upstream/README_ZH_CN.md"
    )
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "mgba" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/doc/mGBA" TYPE FILE FILES
    "/home/bill/ironmon_emu/mgba-android-memapi/upstream/CHANGES"
    "/home/bill/ironmon_emu/mgba-android-memapi/upstream/LICENSE"
    )
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  # Include the install script for each subdirectory.
  include("/home/bill/ironmon_emu/mgba-android-memapi/build-desktop/src/debugger/cmake_install.cmake")
  include("/home/bill/ironmon_emu/mgba-android-memapi/build-desktop/src/feature/cmake_install.cmake")
  include("/home/bill/ironmon_emu/mgba-android-memapi/build-desktop/src/script/cmake_install.cmake")
  include("/home/bill/ironmon_emu/mgba-android-memapi/build-desktop/src/arm/cmake_install.cmake")
  include("/home/bill/ironmon_emu/mgba-android-memapi/build-desktop/src/core/cmake_install.cmake")
  include("/home/bill/ironmon_emu/mgba-android-memapi/build-desktop/src/gb/cmake_install.cmake")
  include("/home/bill/ironmon_emu/mgba-android-memapi/build-desktop/src/gba/cmake_install.cmake")
  include("/home/bill/ironmon_emu/mgba-android-memapi/build-desktop/src/sm83/cmake_install.cmake")
  include("/home/bill/ironmon_emu/mgba-android-memapi/build-desktop/src/util/cmake_install.cmake")
  include("/home/bill/ironmon_emu/mgba-android-memapi/build-desktop/test/cmake_install.cmake")

endif()

if(CMAKE_INSTALL_COMPONENT)
  set(CMAKE_INSTALL_MANIFEST "install_manifest_${CMAKE_INSTALL_COMPONENT}.txt")
else()
  set(CMAKE_INSTALL_MANIFEST "install_manifest.txt")
endif()

string(REPLACE ";" "\n" CMAKE_INSTALL_MANIFEST_CONTENT
       "${CMAKE_INSTALL_MANIFEST_FILES}")
file(WRITE "/home/bill/ironmon_emu/mgba-android-memapi/build-desktop/${CMAKE_INSTALL_MANIFEST}"
     "${CMAKE_INSTALL_MANIFEST_CONTENT}")
