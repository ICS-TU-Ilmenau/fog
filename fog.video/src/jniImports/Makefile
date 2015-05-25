#-------------------------------------------------------------------------------
# Forwarding on Gates Simulator/Emulator - Video and audio Gates
# Copyright (c) 2015, Integrated Communication Systems Group, TU Ilmenau.
# 
# This part of the Forwarding on Gates Simulator/Emulator is free software.
# Your are allowed to redistribute it and/or modify it under the terms of
# the GNU General Public License version 2 as published by the Free Software
# Foundation.
# 
# This source is published in the hope that it will be useful, but
# WITHOUT ANY WARRANTY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU General Public License version 2 for more details.
# 
# You should have received a copy of the GNU General Public License version 2
# along with this program. Otherwise, you can write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02111, USA.
# Alternatively, you find an online version of the license text under
# http://www.gnu.org/licenses/gpl-2.0.html.
#-------------------------------------------------------------------------------
###############################################################################
# MODIFY AS NEEDED !
# path configuration, depending on the current build system
ARCH_BITS:= 			$(shell getconf LONG_BIT)
JDK_PATH=               $(JDK_HOME)
OSX_JDK_INCLUDE=		/System/Library/Frameworks/JavaVM.framework/Headers
HOMER_PATH=             /mnt/git/Homer-Conferencing
#../../../Homer-Conferencing
NAPI_LIB_PATH=			$(HOMER_PATH)/HomerNAPI
MEDIA_LIB_PATH=			$(HOMER_PATH)/HomerMultimedia
BASE_LIB_PATH=			$(HOMER_PATH)/HomerBase
MONITOR_LIB_PATH=		$(HOMER_PATH)/HomerMonitor
FFMPEG_PATH=			$(HOMER_PATH)/../Homer-Release/HomerIncludes/
RM=                     rm -rf
GENERAL_FLAGS=			-O0 -g3 -Wall -c -fmessage-length=0 -D_GNU_SOURCE -D__STDC_CONSTANT_MACROS

ifeq ($(MSYSTEM),MINGW32)
# Windows found
# MODIFY THIS IN ORDER TO SELECT BETWEEN 32/64 bit builds
	ARCH_BITS:= 			64
	LIB_TARGET=				../../libs/win$(ARCH_BITS)/MultimediaJni.dll
	JDK_PATH=				/mnt/jdk
	INCLUDES= 				-I. -I$(MEDIA_LIB_PATH)/include/ -I$(BASE_LIB_PATH)/include/ -I$(BASE_LIB_PATH)/include/Logging/ -I$(MONITOR_LIB_PATH)/include/ -I$(NAPI_LIB_PATH)/include/ -I$(FFMPEG_PATH) -I$(JDK_PATH)/include -I$(JDK_PATH)/include/win32
	FLAGS= 					$(GENERAL_FLAGS) -DWINDOWS -m$(ARCH_BITS)
else
	ifneq (,$(findstring xquartz,$(DISPLAY)))
		# OSX found
		LIB_TARGET=				../../libs/osx$(ARCH_BITS)/libMultimediaJni.dylib
	else
		# Linux found
		LIB_TARGET=				../../libs/linux$(ARCH_BITS)/libMultimediaJni.so
	endif
	INCLUDES= 				-I./ -I$(MEDIA_LIB_PATH)/include/ -I$(BASE_LIB_PATH)/include/ -I$(BASE_LIB_PATH)/include/Logging/ -I$(MONITOR_LIB_PATH)/include/ -I$(NAPI_LIB_PATH)/include/ -I$(FFMPEG_PATH) -I$(JDK_PATH)/include -I$(JDK_PATH)/include/linux -I$(OSX_JDK_INCLUDE)
	FLAGS= 					$(GENERAL_FLAGS) -DLINUX -fPIC -m$(ARCH_BITS)
endif

# HINT: http://java.sun.com/products/jdk/faq/jni-j2sdk-faq.html#compiler -> insert "#define __int64 long long int" in "jni_md.h"

###############################################################################
# DON'T MODIFY ANYTHING FROM HERE
all : VideoDecoderProxy.o VideoTranscoderProxy.o
ifeq ($(MSYSTEM),MINGW32)
	# WINDOWS
	g++ -L../../libs/win$(ARCH_BITS) -Wl,--enable-stdcall-fixup -Xlinker VideoPluginProxy.def -shared -o $(LIB_TARGET) VideoDecoderProxy.o VideoTranscoderProxy.o -lHomerBase -lHomerMonitor -lHomerMultimedia
else
ifneq (,$(findstring xquartz,$(DISPLAY)))
	# OSX
	g++ -L../../libs/osx$(ARCH_BITS) -shared -m$(ARCH_BITS) -o $(LIB_TARGET) VideoDecoderProxy.o VideoTranscoderProxy.o -lHomerBase -lHomerMonitor -lHomerMultimedia
else
	# LINUX
	g++ -L../../libs/linux$(ARCH_BITS) -shared -m$(ARCH_BITS) -o $(LIB_TARGET) VideoDecoderProxy.o VideoTranscoderProxy.o -lHomerBase -lHomerMonitor -lHomerMultimedia
endif
endif
	
VideoDecoderProxy.o :
	g++ $(INCLUDES) $(FLAGS) VideoDecoderProxy.C

VideoTranscoderProxy.o :
	g++ $(INCLUDES) $(FLAGS) VideoTranscoderProxy.C

###############################################################################
# make targets for clean up purposes 
clean::
	$(RM) $(LIB_TARGET)

cleaner: clean
	$(RM) *.bak
	$(RM) *.dep
	$(RM) *.o

###############################################################################
	