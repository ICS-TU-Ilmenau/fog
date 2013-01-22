/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Video and audio Gates
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 *
 * This part of the Forwarding on Gates Simulator/Emulator is free software.
 * Your are allowed to redistribute it and/or modify it under the terms of
 * the GNU General Public License version 2 as published by the Free Software
 * Foundation.
 *
 * This source is published in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License version 2 for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this program. Otherwise, you can write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02111, USA.
 * Alternatively, you find an online version of the license text under
 * http://www.gnu.org/licenses/gpl-2.0.html.
 ******************************************************************************/
#include <jni.h> 
#include <VideoTranscoderProxy.h>
#include <string>

#ifdef LINUX
#include <unistd.h>
#endif

#ifdef WIN32
#include <windows.h>
#endif

#include <MediaSourceMem.h>
#include <MediaSourceMuxer.h>
#include <MediaSinkMem.h>
#include <MediaSink.h>
#include <ProcessStatisticService.h>
#include <Logger.h>
#include <LogSinkFile.h>
#include <HBTime.h>

#include <map>

// maximum packet size
#define MAX_PACKET_SIZE								64*1024
#define MAX_FRAME_SIZE                              1920 * 1080 * 4 // HDTV of format RGBA

using namespace std;
using namespace Homer::Base;
using namespace Homer::Multimedia;
using namespace Homer::Monitor;

struct Transcoder{
    MediaSourceMuxer	*muxer;
    MediaSourceMem		*source;
    MediaSinkMem		*storage;
    char 		   		*frameBuffer;
    char				*packetBuffer;
};

//! number of streams (total, not current)
static int sInstanceCount = 0;

//! map from id to source
static map<int, Transcoder> sTranscoder;

int initTranscoderInstance()
{
    int tInstanceHandle = sInstanceCount;
    sInstanceCount++;

    Socket::DisableIPv6Support();
	SVC_PROCESS_STATISTIC.DisableProcessStatisticSupport();
	sTranscoder[tInstanceHandle].source = new MediaSourceMem(true);
	sTranscoder[tInstanceHandle].muxer = new MediaSourceMuxer(sTranscoder[tInstanceHandle].source);
    sTranscoder[tInstanceHandle].storage = new MediaSinkMem("MemorySink", MEDIA_SINK_VIDEO, true /* assume RTP is always activated */);
    sTranscoder[tInstanceHandle].muxer->RegisterMediaSink(sTranscoder[tInstanceHandle].storage);
    sTranscoder[tInstanceHandle].frameBuffer = (char*)malloc(MAX_FRAME_SIZE);
    if (sTranscoder[tInstanceHandle].frameBuffer == NULL)
    	printf("Unable to allocate memory for transcoder frame buffer\n");
    sTranscoder[tInstanceHandle].packetBuffer = (char*)malloc(MAX_PACKET_SIZE);
    if (sTranscoder[tInstanceHandle].packetBuffer == NULL)
    	printf("Unable to allocate memory for transcoder packet buffer\n");

    return tInstanceHandle;
}

JNIEXPORT void JNICALL Java_jniImports_VideoTranscoder_open(JNIEnv *env, jobject, jint pHandle, jstring pInputCodec, jboolean pRtp, jstring pOutputCodec, jint xRes, jint yRes, jfloat fps)
{
	const char * tInputCodec = (*env).GetStringUTFChars(pInputCodec, 0);
	const char * tOutputCodec = (*env).GetStringUTFChars(pOutputCodec, 0);

	// define input stream parameters
	sTranscoder[pHandle].source->SetInputStreamPreferences(string(tInputCodec), false, pRtp);

    // define output stream parameters
    sTranscoder[pHandle].muxer->SetOutputStreamPreferences(tOutputCodec, 10 /* output quality */, 1200 /* max. packet size */, false /* no immediate reset */, xRes, yRes, pRtp);

    // open muxer (which auto. opens the original source)
	sTranscoder[pHandle].muxer->OpenVideoGrabDevice(xRes, yRes, fps);
}

JNIEXPORT jint JNICALL Java_jniImports_VideoTranscoder_getInstance(JNIEnv *env, jobject)
{
    int tHandle = initTranscoderInstance();

	return tHandle;
}

JNIEXPORT void JNICALL Java_jniImports_VideoTranscoder_stop(JNIEnv *env, jobject,  jint pHandle)
{
    sTranscoder[pHandle].muxer->StopGrabbing();
    sTranscoder[pHandle].storage->StopProcessing();
}

JNIEXPORT void JNICALL Java_jniImports_VideoTranscoder_close(JNIEnv *env, jobject,	jint pHandle)
{
    sTranscoder[pHandle].muxer->StopGrabbing();
	sTranscoder[pHandle].storage->StopProcessing();
    sTranscoder[pHandle].muxer->UnregisterMediaSink(sTranscoder[pHandle].storage);

    //HINT: no need for delete sTranscoder[pHandle].storage because this is done within UnregisterMediaSink
    delete sTranscoder[pHandle].muxer;
    delete sTranscoder[pHandle].source;
	free(sTranscoder[pHandle].frameBuffer);
	free(sTranscoder[pHandle].packetBuffer);
}

JNIEXPORT void JNICALL Java_jniImports_VideoTranscoder_addDataInput(JNIEnv *env, jobject obj, jint pHandle, jbyteArray ba, jint size)
{
	jbyte* tBuffer = (*env).GetByteArrayElements(ba, 0);

	sTranscoder[pHandle].source->WriteFragment((char*)tBuffer, size);

	(*env).ReleaseByteArrayElements(ba, tBuffer, 0);
}

JNIEXPORT jbyteArray JNICALL Java_jniImports_VideoTranscoder_getOutputPacket(JNIEnv *pEnv, jobject obj, jint pHandle)
{
    int tBufferSize = MAX_PACKET_SIZE;
    char *tBuffer = sTranscoder[pHandle].packetBuffer;

    sTranscoder[pHandle].storage->ReadFragment(tBuffer, tBufferSize);

    //printf("Buffer sizes: decoder is %d of %d, encoder is %d of %d, output is %d of %d\n", sTranscoder[pHandle].source->GetFragmentBufferCounter(), sTranscoder[pHandle].source->GetFragmentBufferSize(), sTranscoder[pHandle].muxer->GetMuxingBufferCounter(), sTranscoder[pHandle].muxer->GetMuxingBufferSize(), sTranscoder[pHandle].storage->GetFragmentBufferCounter(), sTranscoder[pHandle].storage->GetFragmentBufferSize());

    if(tBufferSize == 0)
        printf("Read zero packet\n");

    jbyteArray tResult = pEnv->NewByteArray(tBufferSize);
    pEnv->SetByteArrayRegion(tResult, 0, tBufferSize, (jbyte*)tBuffer);

    return tResult;
}

JNIEXPORT jbyteArray JNICALL Java_jniImports_VideoTranscoder_getFrame(JNIEnv *pEnv, jobject obj, jint pHandle)
{
    int tBufferSize = MAX_FRAME_SIZE;
    char *tBuffer = sTranscoder[pHandle].frameBuffer;

    if (sTranscoder[pHandle].muxer->GrabChunk(tBuffer, tBufferSize) <= 0)
    {
        printf("Failed to grab new chunk\n");
        return NULL;
    }

    //printf("Buffer sizes: decoder is %d of %d, encoder is %d of %d, output is %d of %d\n", sTranscoder[pHandle].source->GetFragmentBufferCounter(), sTranscoder[pHandle].source->GetFragmentBufferSize(), sTranscoder[pHandle].muxer->GetMuxingBufferCounter(), sTranscoder[pHandle].muxer->GetMuxingBufferSize(), sTranscoder[pHandle].storage->GetFragmentBufferCounter(), sTranscoder[pHandle].storage->GetFragmentBufferSize());

    if(tBufferSize == 0)
        printf("Read zero packet\n");

    jbyteArray tResult = pEnv->NewByteArray(tBufferSize);
    pEnv->SetByteArrayRegion(tResult, 0, tBufferSize, (jbyte*)tBuffer);

    return tResult;
}

JNIEXPORT void JNICALL Java_jniImports_VideoTranscoder_getStats(JNIEnv *env, jobject obj, jint pHandle, jintArray ia)
{
	jint* stats = (*env).GetIntArrayElements(ia, 0);
	int len = (*env).GetArrayLength(ia);
	int* aStats = (int*) malloc(4 * 7);

	aStats[0] = sTranscoder[pHandle].source->GetMaxPacketSize();
	aStats[1] = sTranscoder[pHandle].source->GetAvgDataRate();
	aStats[2] = sTranscoder[pHandle].source->GetPacketCount();
	aStats[3] = sTranscoder[pHandle].source->GetMinPacketSize();
	aStats[4] = sTranscoder[pHandle].source->GetLostPacketCount();
	aStats[6] = sTranscoder[pHandle].source->GetAvgPacketSize();

	for (int i = 0; i < min(len, 7); i++)
		stats[i] = aStats[i];

	(*env).ReleaseIntArrayElements(ia, stats, 0);
}

JNIEXPORT void JNICALL Java_jniImports_VideoTranscoder_InitLogger(JNIEnv *pEnv, jobject, jint pLogLevel)
{
    LOGGER.SetColoring(false);
	LOGGER.Init(pLogLevel);
}

