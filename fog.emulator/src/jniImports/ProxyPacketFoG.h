/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - emulator interface
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class de_tuilmenau_ics_fog_emulator_PacketFoGWrapper */

#ifndef _PacketFoG_
#define _PacketFoG_
#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     de_tuilmenau_ics_fog_emulator_PacketFoG
 * Method:    getInstance
 * Signature: ()I
 */
JNIEXPORT int JNICALL Java_jniImports_PacketFoG_getInstance
  (JNIEnv *, jobject);

/*
 * Class:     de_tuilmenau_ics_fog_emulator_PacketFoG
 * Method:    SetEthernetSourceAdr
 * Signature: (ILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_jniImports_PacketFoG_DoSetEthernetSourceAdr
  (JNIEnv *, jobject, int, jstring);

/*
 * Class:     de_tuilmenau_ics_fog_emulator_PacketFoG
 * Method:    SetEthernetDestinationAdr
 * Signature: (ILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_jniImports_PacketFoG_DoSetEthernetDestinationAdr
  (JNIEnv *, jobject, int, jstring);

/*
 * Class:     de_tuilmenau_ics_fog_emulator_PacketFoG
 * Method:    SetFoGPayload
 * Signature: (I[BI)V
 */
JNIEXPORT void JNICALL Java_jniImports_PacketFoG_DoSetFoGPayload
  (JNIEnv *, jobject, int, jbyteArray, jint);

/*
 * Class:     de_tuilmenau_ics_fog_emulator_PacketFoG
 * Method:    SetFoGRoute
 * Signature: (I[BI)V
 */
JNIEXPORT void JNICALL Java_jniImports_PacketFoG_DoSetFoGRoute
  (JNIEnv *, jobject, int, jbyteArray, jint);

/*
 * Class:     de_tuilmenau_ics_fog_emulator_PacketFoG
 * Method:    SetFoGReverseRoute
 * Signature: (I[BI)V
 */
JNIEXPORT void JNICALL Java_jniImports_PacketFoG_DoSetFoGReverseRoute
  (JNIEnv *, jobject, int, jbyteArray, jint);

/*
 * Class:     de_tuilmenau_ics_fog_emulator_PacketFoG
 * Method:    SetFoGAuthentications
 * Signature: (I[BI)V
 */
JNIEXPORT void JNICALL Java_jniImports_PacketFoG_DoSetFoGAuthentications
  (JNIEnv *, jobject, int, jbyteArray, jint);


/*
 * Class:     de_tuilmenau_ics_fog_emulator_PacketFoG
 * Method:    SetFoGMarkingSignaling
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_jniImports_PacketFoG_DoSetFoGMarkingSignaling
  (JNIEnv *, jobject, int);

/*
 * Class:     de_tuilmenau_ics_fog_emulator_PacketFoG
 * Method:    SetFoGMarkingFragment
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_jniImports_PacketFoG_DoSetFoGMarkingFragment
  (JNIEnv *, jobject, int);

/*
 * Class:     de_tuilmenau_ics_fog_emulator_PacketFoG
 * Method:    Reset
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_jniImports_PacketFoG_DoReset
  (JNIEnv *, jobject, int);

/*
 * Class:     de_tuilmenau_ics_fog_emulator_PacketFoG
 * Method:    Send
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_jniImports_PacketFoG_DoSend
  (JNIEnv *, jobject, int);

/*
 * Class:     de_tuilmenau_ics_fog_emulator_PacketFoG
 * Method:    GetDefaultDevice
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_jniImports_PacketFoG_DoGetDefaultDevice
  (JNIEnv *, jobject, int);

/*
 * Class:     de_tuilmenau_ics_fog_emulator_PacketFoG
 * Method:    SetSendDevice
 * Signature: (ILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_jniImports_PacketFoG_DoSetSendDevice
  (JNIEnv *, jobject, int, jstring);

/*
 * Class:     de_tuilmenau_ics_fog_emulator_PacketFoG
 * Method:    GetSendDevice
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_jniImports_PacketFoG_DoGetSendDevice
  (JNIEnv *, jobject, int);

/*
 * Class:     de_tuilmenau_ics_fog_emulator_PacketFoG
 * Method:    SetReceiveDevice
 * Signature: (ILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_jniImports_PacketFoG_DoSetReceiveDevice
  (JNIEnv *, jobject, int, jstring);

/*
 * Class:     de_tuilmenau_ics_fog_emulator_PacketFoG
 * Method:    GetReceiveDevice
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_jniImports_PacketFoG_DoGetReceiveDevice
  (JNIEnv *, jobject, int);

/*
 * Class:     de_tuilmenau_ics_fog_emulator_PacketFoG
 * Method:    PrepareReceive
 * Signature: (ILjava/lang/String;IZ)Z
 */
JNIEXPORT jboolean JNICALL Java_jniImports_PacketFoG_DoPrepareReceive
  (JNIEnv *, jobject, int, jstring, jint, jboolean);

/*
 * Class:     de_tuilmenau_ics_fog_emulator_PacketFoG
 * Method:    GetFoGPayload
 * Signature: (I)[B
 */
JNIEXPORT jbyteArray JNICALL Java_jniImports_PacketFoG_DoGetFoGPayload
    (JNIEnv *, jobject, int);

/*
 * Class:     de_tuilmenau_ics_fog_emulator_PacketFoG
 * Method:    IsLastFragment
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_jniImports_PacketFoG_DoIsLastFragment
  (JNIEnv *, jobject, int);

/*
 * Class:     de_tuilmenau_ics_fog_emulator_PacketFoG
 * Method:    GetEthernetSourceAdr
 * Signature: (I)[B
 */
JNIEXPORT jbyteArray JNICALL Java_jniImports_PacketFoG_DoGetEthernetSourceAdr
    (JNIEnv *, jobject, int);

/*
 * Class:     de_tuilmenau_ics_fog_emulator_PacketFoG
 * Method:    GetDestinationAdr
 * Signature: (I)[B
 */
JNIEXPORT jbyteArray JNICALL Java_jniImports_PacketFoG_DoGetEthernetDestinationAdr
    (JNIEnv *, jobject, int);

/*
 * Class:     de_tuilmenau_ics_fog_emulator_PacketFoG
 * Method:    Receive
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_jniImports_PacketFoG_DoReceive
  (JNIEnv *, jobject, int);

#ifdef __cplusplus
}
#endif
#endif
