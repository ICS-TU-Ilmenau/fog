#-------------------------------------------------------------------------------
# Forwarding on Gates Simulator/Emulator - emulator interface
# Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
# 
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html.
#-------------------------------------------------------------------------------
###############################################################################
# MODIFY AS NEEDED !
# path configuration, depending on the current build system
ARCH_BITS:= 			$(shell getconf LONG_BIT)
JDK_PATH=				$(JDK_HOME)
OSX_JDK_INCLUDE=		/System/Library/Frameworks/JavaVM.framework/Headers
TOPDIR=					.
BASE_LIB_PATH=			../../../../HomerBase
NETINJECT_LIB_PATH=		../../../LibNetInject
RM=						rm -rf
GENERAL_FLAGS= 			-O0 -g3 -Wall -c -fmessage-length=0 -D_GNU_SOURCE -D__STDC_CONSTANT_MACROS

ifeq ($(MSYSTEM),MINGW32)
ARCH_BITS:=				32
LIB_TARGET=				../../libs/win32/EthernetJni.dll
JDK_PATH=				/mnt/jdk
INCLUDES= 				-I. -I$(BASE_LIB_PATH)/include/ -I$(BASE_LIB_PATH)/include/Logging/ -I$(JDK_PATH)/include -I$(JDK_PATH)/include/win32 -I $(NETINJECT_LIB_PATH)/include -I $(NETINJECT_LIB_PATH)/include/Base -I ../../libs/include -I ../../libs/include/pcap
FLAGS= 					$(GENERAL_FLAGS) -DWIN32 -mwin32
else
LIB_TARGET=				../../libs/osx$(ARCH_BITS)/libEthernetJni.dylib
#LIB_TARGET=			../../libs/linux$(ARCH_BITS)/libEthernetJni.so
INCLUDES= 				-I./ -I$(BASE_LIB_PATH)/include/ -I$(BASE_LIB_PATH)/include/Logging/ -I$(JDK_PATH)/include -I$(JDK_PATH)/include/linux -I$(NETINJECT_LIB_PATH)/include -I$(NETINJECT_LIB_PATH)/include/Base -I$(OSX_JDK_INCLUDE)
 
FLAGS= 					$(GENERAL_FLAGS) -DLINUX -fPIC -m$(ARCH_BITS)
endif

 
###############################################################################
all: ProxyPacketFoG.o	
ifeq ($(MSYSTEM),MINGW32)
	g++ -L../../libs/win$(ARCH_BITS) -L$(NETINJECT_LIB_PATH)/lib -Wl,--enable-stdcall-fixup -Xlinker EmulatorPluginProxy.def -shared -m$(ARCH_BITS) -o$(LIB_TARGET) ProxyPacketFoG.o -lNetInject
else
	g++ -L../../libs/linux$(ARCH_BITS) -L$(NETINJECT_LIB_PATH)/lib -shared -m$(ARCH_BITS) -o $(LIB_TARGET) ProxyPacketFoG.o -lNetInject
endif

ProxyPacketFoG.o:
	g++ $(INCLUDES) $(FLAGS) ProxyPacketFoG.C 

###############################################################################
# make targets for clean up purposes 
clean::
	$(RM) $(LIB_TARGET)

cleaner: clean
	$(RM) *.bak
	$(RM) *.dep
	$(RM) *.o

###############################################################################
