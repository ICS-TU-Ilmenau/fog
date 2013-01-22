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
#include <VideoDecoderProxy.h>
#include <string>

#ifdef LINUX
#include <unistd.h>
#endif

#ifdef WIN32
#include <windows.h>
#endif

#include <MediaSourceMem.h>
#include <ProcessStatisticService.h>
#include <Logger.h>
#include <LogSinkFile.h>
#include <HBTime.h>

#include <map>

#define MAX_FRAME_SIZE                              1920 * 1080 * 4 // HDTV of format RGBA

using namespace std;
using namespace Homer::Base;
using namespace Homer::Multimedia;
using namespace Homer::Monitor;

//! structure with values every videoSource has
struct Decoder {
	MediaSourceMem *source;
    char 		   *frameBuffer;
};

//number of streams (total, not current)
static int sDecoderInstanceCount = 0;

//! map from id to source
static map<int, Decoder> sDecoder;

int initDecoderInstance()
{
    int tInstanceHandle = sDecoderInstanceCount;
    sDecoderInstanceCount++;

	Socket::DisableIPv6Support();
	SVC_PROCESS_STATISTIC.DisableProcessStatisticSupport();

	sDecoder[tInstanceHandle].source = new MediaSourceMem(true);
	sDecoder[tInstanceHandle].frameBuffer = (char*)malloc(MAX_FRAME_SIZE);

	return tInstanceHandle;
}

/**
 * @brief initializes stream and binds the buffer to the source
 */
JNIEXPORT void JNICALL Java_jniImports_VideoDecoder_open(JNIEnv *env, jobject, jint pHandle, jstring codec, jboolean rtp, jbyteArray picBuffer, jintArray stats, jint xRes, jint yRes, jfloat fps)
{
	const char * tCodec = (*env).GetStringUTFChars(codec, 0);
	sDecoder[pHandle].source->SetInputStreamPreferences(string(tCodec), false, rtp);
	sDecoder[pHandle].source->OpenVideoGrabDevice(xRes, yRes, fps);
}

/**
 * @brief initializes the videoSoruce structure with default values
 * @return the port bound for the stream
 *
 * the loglevel is set here
 * LOGGER: LOG_OFF, LOG_ERROR, LOG_INFO, LOG_VERBOSE
 */
JNIEXPORT jint JNICALL Java_jniImports_VideoDecoder_getInstance(JNIEnv *env, jobject)
{
    int tHandle = initDecoderInstance();

    return tHandle;
}

/**
 * brief the grab loop in start will be broken hear
 */
JNIEXPORT void JNICALL Java_jniImports_VideoDecoder_stop(JNIEnv *env, jobject,	jint pHandle)
{
	sDecoder[pHandle].source->StopGrabbing();
}

/**
 * brief the grab loop in start will be broken hear
 */
JNIEXPORT void JNICALL Java_jniImports_VideoDecoder_close(JNIEnv *env, jobject,	jint pHandle)
{
	sDecoder[pHandle].source->StopGrabbing();

	delete sDecoder[pHandle].source;
	free(sDecoder[pHandle].frameBuffer);
}

/**
 * @brief returns the buffer content through the Java Native Interface, if a new picture was transfered the method returns true
 */
JNIEXPORT void JNICALL Java_jniImports_VideoDecoder_addDataInput(JNIEnv *env, jobject obj, jint pHandle, jbyteArray ba, jint size)
{
	jbyte* tBuffer = (*env).GetByteArrayElements(ba, 0);
	sDecoder[pHandle].source->WriteFragment((char*)tBuffer, size);
	(*env).ReleaseByteArrayElements(ba, tBuffer, 0);
}

/**
 * @brief returns the buffer content through the Java Native Interface, if a new picture was transfered the method returns true
 */
JNIEXPORT jbyteArray JNICALL Java_jniImports_VideoDecoder_getFrame(JNIEnv *pEnv, jobject obj, jint pHandle)
{
    int tBufferSize = MAX_FRAME_SIZE;
    char *tBuffer = sDecoder[pHandle].frameBuffer;

	if (sDecoder[pHandle].source->GrabChunk(tBuffer, tBufferSize) <= 0)
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

/**
 * @brief returns the status information buffer through the Java Native Interface
 */
JNIEXPORT void JNICALL Java_jniImports_VideoDecoder_getStats(JNIEnv *env, jobject obj, jint pHandle, jintArray ia)
{
	jint* stats = (*env).GetIntArrayElements(ia, 0);
	int len = (*env).GetArrayLength(ia);
	int* aStats = (int*) malloc(4 * 7);

	aStats[0] = sDecoder[pHandle].source->GetMaxPacketSize();
	aStats[1] = sDecoder[pHandle].source->GetAvgDataRate();
	aStats[2] = sDecoder[pHandle].source->GetPacketCount();
	aStats[3] = sDecoder[pHandle].source->GetMinPacketSize();
	aStats[4] = sDecoder[pHandle].source->GetLostPacketCount();
	//aStats[5] = sDecoder[pHandle].grabTime;
	aStats[6] = sDecoder[pHandle].source->GetAvgPacketSize();

	for (int i = 0; i < min(len, 7); i++)
		stats[i] = aStats[i];

	(*env).ReleaseIntArrayElements(ia, stats, 0);
}

// started new interface - work in progress
JNIEXPORT void JNICALL Java_jniImports_VideoDecoder_InitLogger(JNIEnv *pEnv, jobject, jint pLogLevel)
{
    LOGGER.SetColoring(false);
	LOGGER.Init(pLogLevel);
}

