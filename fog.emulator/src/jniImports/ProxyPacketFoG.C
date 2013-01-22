/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - emulator interface
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
#include <jni.h>
#include <ProxyPacketFoG.h>
#include "../../LibNetInject/include/Base/HBLogger.h"
#include "../../LibNetInject/include/Base/HBTime.h"
#include "../../LibNetInject/include/PacketFoG.h"
#include <iostream>
#include <sstream>
#include <map>

using namespace FoG::Net;
using namespace Homer::Base;
using namespace std;

#define MAX_ETH_PAYLOAD_SIZE 1500
#define ETH_ADR_SIZE         6

struct PacketFogInstance{
    PacketFoG *Sender;
    PacketFoG *Receiver;
};

//! number of streams (total, not current)
static int sInstanceCount = 0;

//! map from id to source
static map<int, PacketFogInstance> sPacketFogInstance;

int initPacketFogInstance()
{
    int tInstanceHandle = sInstanceCount;
    sInstanceCount++;

    sPacketFogInstance[tInstanceHandle].Sender = new PacketFoG();
    sPacketFogInstance[tInstanceHandle].Receiver = new PacketFoG();

    return tInstanceHandle;
}

JNIEXPORT int JNICALL Java_jniImports_PacketFoG_getInstance
  (JNIEnv *pEnv, jobject pObj)
{
    int tHandle = initPacketFogInstance();

    return tHandle;
}

JNIEXPORT void JNICALL Java_jniImports_PacketFoG_DoSetEthernetSourceAdr
  (JNIEnv *pEnv, jobject pObj, jint pHandle, jstring pAddress)
{
	const char * c_string = pEnv->GetStringUTFChars(pAddress, 0);
	stringstream tStrStream;
	tStrStream<<c_string;
	sPacketFogInstance[pHandle].Sender->SetEthernetSourceAdr(tStrStream.str());
}

JNIEXPORT void JNICALL Java_jniImports_PacketFoG_DoSetEthernetDestinationAdr
  (JNIEnv *pEnv, jobject pObj, jint pHandle, jstring pAddress)
{
	const char *c_string = pEnv->GetStringUTFChars(pAddress, 0);
	stringstream tStrStream;
	tStrStream<<c_string;
	sPacketFogInstance[pHandle].Sender->SetEthernetDestinationAdr(tStrStream.str());
}

JNIEXPORT void JNICALL Java_jniImports_PacketFoG_DoSetFoGPayload
   (JNIEnv *pEnv, jobject pObj, jint pHandle, jbyteArray pPayload, jint pPayloadSize)
{
    char tPayload[MAX_ETH_PAYLOAD_SIZE];
    pEnv->GetByteArrayRegion(pPayload, 0, pPayloadSize, (jbyte*)tPayload);
    sPacketFogInstance[pHandle].Sender->SetFoGPayload(tPayload, pPayloadSize);
}

JNIEXPORT void JNICALL Java_jniImports_PacketFoG_DoSetFoGRoute
   (JNIEnv *pEnv, jobject pObj, jint pHandle, jbyteArray pRoute, jint pRouteSize)
{
    char tRoute[MAX_ETH_PAYLOAD_SIZE];
    pEnv->GetByteArrayRegion(pRoute, 0, pRouteSize, (jbyte*)tRoute);
    sPacketFogInstance[pHandle].Sender->SetFoGRoute(tRoute, pRouteSize);
}

JNIEXPORT void JNICALL Java_jniImports_PacketFoG_DoSetFoGReverseRoute
   (JNIEnv *pEnv, jobject pObj, jint pHandle, jbyteArray pRoute, jint pRouteSize)
{
    char tRoute[MAX_ETH_PAYLOAD_SIZE];
    pEnv->GetByteArrayRegion(pRoute, 0, pRouteSize, (jbyte*)tRoute);
    sPacketFogInstance[pHandle].Sender->SetFoGReverseRoute(tRoute, pRouteSize);
}

JNIEXPORT void JNICALL Java_jniImports_PacketFoG_DoSetFoGAuthentications
   (JNIEnv *pEnv, jobject pObj, jint pHandle, jbyteArray pAuths, jint pAuthsSize)
{
    char tAuths[MAX_ETH_PAYLOAD_SIZE];
    pEnv->GetByteArrayRegion(pAuths, 0, pAuthsSize, (jbyte*)tAuths);
    sPacketFogInstance[pHandle].Sender->SetFoGAuthentications(tAuths, pAuthsSize);
}

JNIEXPORT void JNICALL Java_jniImports_PacketFoG_DoReset
  (JNIEnv *pEnv, jobject pObj, jint pHandle)
{
    sPacketFogInstance[pHandle].Sender->Reset(true);
}

JNIEXPORT void JNICALL Java_jniImports_PacketFoG_DoSetFoGMarkingSignaling
  (JNIEnv *pEnv, jobject pObj, jint pHandle)
{
    sPacketFogInstance[pHandle].Sender->SetFoGMarkingSignaling();
}

JNIEXPORT void JNICALL Java_jniImports_PacketFoG_DoSetFoGMarkingFragment
  (JNIEnv *pEnv, jobject pObj, jint pHandle)
{
    sPacketFogInstance[pHandle].Sender->SetFoGMarkingFragment();
}

JNIEXPORT jint JNICALL Java_jniImports_PacketFoG_DoSend
  (JNIEnv *pEnv, jobject pObj, jint pHandle)
{
	return sPacketFogInstance[pHandle].Sender->Send();
}

// ################################################################################

JNIEXPORT jstring JNICALL Java_jniImports_PacketFoG_DoGetDefaultDevice
  (JNIEnv *pEnv, jobject pObj, jint pHandle)
{
    jstring joutput;
    std::string s = sPacketFogInstance[pHandle].Receiver->GetDefaultDevice();
    joutput=pEnv->NewStringUTF(s.c_str());
    return joutput;
}

JNIEXPORT void JNICALL Java_jniImports_PacketFoG_DoSetSendDevice
    (JNIEnv *pEnv, jobject pObj, jint pHandle, jstring pDeviceName)
{
    const char * c_string = pEnv->GetStringUTFChars(pDeviceName, 0);
    stringstream tStrStream;
    tStrStream<<c_string;
    sPacketFogInstance[pHandle].Sender->SetSendDevice(tStrStream.str());
}

JNIEXPORT jstring JNICALL Java_jniImports_PacketFoG_DoGetSendDevice
  (JNIEnv *pEnv, jobject pObj, jint pHandle)
{
    jstring joutput;
    std::string s = sPacketFogInstance[pHandle].Sender->GetSendDevice();
    joutput=pEnv->NewStringUTF(s.c_str());
    return joutput;
}

JNIEXPORT void JNICALL Java_jniImports_PacketFoG_DoSetReceiveDevice
    (JNIEnv *pEnv, jobject pObj, jint pHandle, jstring pDeviceName)
{
    const char * c_string = pEnv->GetStringUTFChars(pDeviceName, 0);
    stringstream tStrStream;
    tStrStream<<c_string;
    sPacketFogInstance[pHandle].Receiver->SetReceiveDevice(tStrStream.str());
}

JNIEXPORT jstring JNICALL Java_jniImports_PacketFoG_DoGetReceiveDevice
    (JNIEnv *pEnv, jobject pObj, jint pHandle)
{
    jstring tResult;
    std::string s = sPacketFogInstance[pHandle].Receiver->GetReceiveDevice();
    tResult=pEnv->NewStringUTF(s.c_str());
    return tResult;

}

// ####################################################################################

JNIEXPORT jboolean JNICALL Java_jniImports_PacketFoG_DoPrepareReceive
  (JNIEnv *pEnv, jobject pObj, jint pHandle, jstring pFilter, jint pTimeout, jboolean pAllowForeign)
{
    const char * c_string = pEnv->GetStringUTFChars(pFilter, 0);
    stringstream tStrStream;
    tStrStream<<c_string;
    return sPacketFogInstance[pHandle].Receiver->PrepareReceive(tStrStream.str(), pTimeout, pAllowForeign);
}

JNIEXPORT jbyteArray JNICALL Java_jniImports_PacketFoG_DoGetFoGPayload
   (JNIEnv *pEnv, jobject pObj, jint pHandle)
{
    char tPayload[MAX_ETH_PAYLOAD_SIZE];
    unsigned int tPayloadSize = MAX_ETH_PAYLOAD_SIZE;
    sPacketFogInstance[pHandle].Receiver->GetFoGPayload(&tPayload[0], tPayloadSize);
    jbyteArray tResult = pEnv->NewByteArray(tPayloadSize);
    pEnv->SetByteArrayRegion(tResult, 0, tPayloadSize, (jbyte*)tPayload);
    return tResult;
}

JNIEXPORT jbyteArray JNICALL Java_jniImports_PacketFoG_DoGetEthernetSourceAdr
   (JNIEnv *pEnv, jobject pObj, jint pHandle)
{
    char tAdr[ETH_ADR_SIZE];
    unsigned int tAdrSize = ETH_ADR_SIZE;
    sPacketFogInstance[pHandle].Receiver->GetEthernetSourceAdr(&tAdr[0], tAdrSize);
    jbyteArray tResult = pEnv->NewByteArray(tAdrSize);
    pEnv->SetByteArrayRegion(tResult, 0, tAdrSize, (jbyte*)tAdr);
    return tResult;
}

JNIEXPORT jbyteArray JNICALL Java_jniImports_PacketFoG_DoGetEthernetDestinationAdr
   (JNIEnv *pEnv, jobject pObj, jint pHandle)
{
    char tAdr[ETH_ADR_SIZE];
    unsigned int tAdrSize = ETH_ADR_SIZE;
    sPacketFogInstance[pHandle].Receiver->GetEthernetDestinationAdr(&tAdr[0], tAdrSize);
    jbyteArray tResult = pEnv->NewByteArray(tAdrSize);
    pEnv->SetByteArrayRegion(tResult, 0, tAdrSize, (jbyte*)tAdr);
    return tResult;
}

JNIEXPORT jboolean JNICALL Java_jniImports_PacketFoG_DoIsLastFragment
  (JNIEnv *pEnv, jobject pObj, jint pHandle)
{
    return sPacketFogInstance[pHandle].Receiver->IsLastFragment();
}

JNIEXPORT jboolean JNICALL Java_jniImports_PacketFoG_DoReceive
  (JNIEnv *pEnv, jobject pObj, jint pHandle)
{
    return sPacketFogInstance[pHandle].Receiver->Receive();
}

