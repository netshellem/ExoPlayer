#include "stdio.h"
#include <string.h>
#include <jni.h>
#include <sys/time.h>
#include <drm.h>
#include <ac_drm_core.h>

#include "ac_drm_core.h"
#include "drm.h"
#include "spi.h"

#define CLASS_NAME "com/irdeto/drm/ChinaDrm"

#define _NATIVE_INITIDS "native_initIDs"
#define _NATIVE_STARTUP "native_startup"
#define _NATIVE_SHUTDOWN "native_shutdown"
#define _NATIVE_PLAY "native_play"
#define _NATIVE_PLAY_LOCAL "native_play_local"
#define _NATIVE_QUERY_INFO "native_queryinfo"
#define _NATIVE_PLAY_DASH "native_play_dash"
#define _NATIVE_ACQUIRELICENSE "native_acquireLicense"
#define _NATIVE_ACQUIRELICENSEBYURL "native_acquireLicenseByUrl"
#define _NATIVE_DECRYPTBUFFER "native_decryptBuffer"
#define _NATIVE_SETFORCEQUIT "native_setForceQuit"
#define _NATIVE_CREATE_SESSION "native_createsession"
#define _NATIVE_DESTORY_SESSION "native_destorysession"

#define TAG "drm_jni"

typedef struct _context {
    JavaVM* jvm;
    jobject drm;
} drmContext;

static drmContext gContext;
static jmethodID MID_ERROR_NOTIFICATION = NULL;
static jmethodID MID_DRM_AGENT_REQUEST = NULL;
static jmethodID MID_DRM_REQUEST = NULL;

static ac_drm_session_handle gSessionHandle = NULL;
static ac_drm_decryptParameter gDecryptParameter;
static jobject getConfigInstance(JNIEnv *env, jobject thiz)
{
    jclass cls = env->GetObjectClass(thiz);
    jfieldID fid = env->GetFieldID( cls, "config", "Lcom/irdeto/drm/Config;");
    jobject config = env->GetObjectField( thiz, fid);

    env->DeleteLocalRef(cls);

    return config;
}

static jstring getCreateSessionUserData(JNIEnv *env, jobject configInstance)
{
    jclass cls = env->GetObjectClass( configInstance);
    jmethodID mid = env->GetMethodID( cls, "get_createSession_userData", "()Ljava/lang/String;");
    jstring userData = (jstring)env->CallObjectMethod( configInstance, mid);

    env->DeleteLocalRef(cls);

    return userData;
}

static jstring newStringFromStandardUTF8(JNIEnv *env, const ac_drm_char *str)
{
    if ((env == NULL) || (str == NULL)) return NULL;

    int len = strlen(str);
    jbyteArray bytes = env->NewByteArray( len);
    if (bytes != NULL) {
        env->SetByteArrayRegion( bytes, 0, len, (const jbyte*) str);

        jclass cls = env->FindClass("java/lang/String");
        jmethodID MID_String_init = env->GetMethodID( cls, "<init>", "([BLjava/lang/String;)V");
        jstring charset = env->NewStringUTF( "UTF-8");
        jstring newString = (jstring)env->NewObject(cls, MID_String_init, bytes, charset);

        env->DeleteLocalRef( charset);
        env->DeleteLocalRef( cls);
        env->DeleteLocalRef( bytes);

        return newString;
    }

    return NULL;
}

/**
 * Implement errorNotification defined in drm.h
 *
 */
static void errorNotification(drmErrorMsg* pMsg) {
    drmContext *context = &gContext;
    if (context != NULL) {
        ac_drm_bool needDetach = AC_DRM_FALSE;
        JNIEnv* env = NULL;
        JavaVM* jvm = context->jvm;
        jvm->GetEnv( (void **)&env, JNI_VERSION_1_4);

        if (env == NULL) {
            jvm->AttachCurrentThread( &env, NULL);
            needDetach = AC_DRM_TRUE;
        }

        if (env != NULL) {
            jstring description = newStringFromStandardUTF8(env, (ac_drm_char *)(pMsg->description));
            env->CallVoidMethod( context->drm, MID_ERROR_NOTIFICATION,
                                   (int)pMsg->severity, (int)pMsg->errorCode, (int)pMsg->additionalCode, description);
            env->DeleteLocalRef( description);
        }

        if (needDetach) {
            jvm->DetachCurrentThread();
        }
    }
}

/**
 * Implement drmAgentRequest defined in drm.h
 *
 */
static ac_drm_bool drmAgentRequest(drmRequestMsg* pMsg) {
    ac_drm_bool ret = AC_DRM_TRUE;
    //drmContext *context = (pMsg->pUserData);
    drmContext *context = &gContext;

    LOGD("drm agent request");
    if (context != NULL) {
        ac_drm_bool needDetach = AC_DRM_FALSE;
        JNIEnv* env = NULL;
        JavaVM* jvm = context->jvm;
        jvm->GetEnv( (void **)&env, JNI_VERSION_1_4);

        if (env == NULL) {
            jvm->AttachCurrentThread(&env, NULL);
            needDetach = AC_DRM_TRUE;
        }

        if (env != NULL) {
            jstring url = newStringFromStandardUTF8(env, (ac_drm_char *)(pMsg->pUrl));
            jstring data = NULL;
            if (pMsg->type == AC_DRM_HTTP_METHOD_POST) {
                data = newStringFromStandardUTF8(env, (ac_drm_char *)(pMsg->pPost->pData));
            }

            LOGD("send request {");
            // Get response Data
            jstring resp = (jstring)env->CallObjectMethod( context->drm, MID_DRM_AGENT_REQUEST, url, data);
            const ac_drm_char* pResponse = (resp ? env->GetStringUTFChars( resp, NULL) : NULL);
            ac_drm_uint32 len = (resp ? env->GetStringLength( resp) : 0);
            LOGD("send request } response : %s", pResponse);
            LOGD("dataSize:%d", len);

            pMsg->pResBody->pData = (ac_drm_byte *)(pResponse);
            pMsg->pResBody->dataSize = len;
            if (pResponse==NULL || len==0) {
                ret = AC_DRM_FALSE;
            }

            // Release variables
            env->DeleteLocalRef( url);
            if (data) {
                env->DeleteLocalRef( data);
            }
        }

        if (needDetach) {
            jvm->DetachCurrentThread();
        }
    }

    return ret;
}

/**
 * Implement drmRequest defined in drm.h
 *
 */
static ac_drm_bool drmRequest(drmRequestMsg* pMsg) {
    ac_drm_bool ret = AC_DRM_TRUE;
    //drmContext *context = (pMsg->pUserData);
    drmContext *context = &gContext;

    LOGD("drm request");
    if (context != NULL) {
        ac_drm_bool needDetach = AC_DRM_FALSE;
        JNIEnv* env = NULL;
        JavaVM* jvm = context->jvm;
        jvm->GetEnv( (void **)&env, JNI_VERSION_1_4);

        if (env == NULL) {
            jvm->AttachCurrentThread( &env, NULL);
            needDetach = AC_DRM_TRUE;
        }

        if (env != NULL) {
            jstring url = newStringFromStandardUTF8(env, (ac_drm_char *)(pMsg->pUrl));
            jstring data = NULL;
            if (pMsg->type == AC_DRM_HTTP_METHOD_POST) {
                data = newStringFromStandardUTF8(env, (ac_drm_char *)(pMsg->pPost->pData));
            }

            LOGD("send request {");
            // Get response Data
            jstring resp = (jstring)env->CallObjectMethod( context->drm, MID_DRM_REQUEST, url, data);
            const ac_drm_char* pResponse = (resp ? env->GetStringUTFChars( resp, NULL) : NULL);
            ac_drm_uint32 len = (resp ? env->GetStringLength( resp) : 0);
            LOGD("send request } response : %s", pResponse);
            LOGD("dataSize:%d", len);

            pMsg->pResBody->pData = (ac_drm_byte *)(pResponse);
            pMsg->pResBody->dataSize = len;
            if (pResponse==NULL || len==0) {
                ret = AC_DRM_FALSE;
            }

            // Release variables
            env->DeleteLocalRef( url);
            if (data) {
                env->DeleteLocalRef( data);
            }
        }

        if (needDetach) {
            jvm->DetachCurrentThread();
        }
    }

    return ret;
}

static int getCurrentTime() {
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return tv.tv_sec * 1000 + tv.tv_usec / 1000;
}
#define DECYPT_BLOCK_SIZE 752
static ac_drm_ret play(ac_drm_session_handle handle, const ac_drm_char* pDataFile) {
    ac_drm_ret ret = AC_DRM_SUCCESS;
    // open data File
    FILE *pSrcFile = NULL;
    FILE *pDstFile = NULL;
    ac_drm_uint32 len = 0;
    static ac_drm_char ss[1024]= {0};
    static ac_drm_byte array[DECYPT_BLOCK_SIZE] = {0};
    static ac_drm_byte_buffer buffer = {0};

    ac_drm_decryptParameter decParameter = {0};
    int current = 0;

    decParameter.final = AC_DRM_TRUE;

    current = getCurrentTime();

    LOGD("play {");
    sprintf(ss, "%s_%d.clear", pDataFile, current);

    pSrcFile = fopen(pDataFile, "rb");
    pDstFile = fopen(ss, "wb");
    if (pSrcFile == NULL) {
        LOGD("can not open such file:%s", pDataFile);
        ret = AC_DRM_UNKNOWN_ERROR;
    }

    if (pDstFile == NULL) {
        LOGD("can not open such file:%s", ss);
        ret = AC_DRM_UNKNOWN_ERROR;
    }

    while(ret == AC_DRM_SUCCESS && ((len = fread(array, 1, DECYPT_BLOCK_SIZE, pSrcFile)) > 0)) {
        LOGD("read... %d", len);
        // decrypt data
        LOGD("decryptBuffer {");
        buffer.pData = (ac_drm_byte *)(array);
        buffer.dataSize = len;

        ret = decryptBuffer(handle, &buffer, &decParameter);
        LOGD("decryptBuffer } result = %d", ret);
        if (fwrite(array, 1, buffer.dataSize, pDstFile) < buffer.dataSize) {
            ret = AC_DRM_UNKNOWN_ERROR;
        }
    }

    if (pSrcFile) {
        fclose(pSrcFile);
        pSrcFile = NULL;
    }

    if (pDstFile) {
        fclose(pDstFile);
        pDstFile = NULL;
    }

    LOGD("play } result :%d", ret);
    return ret;
}



/**
 * Native for setting all CallBacks implemented by JAVA
 *
 */
void Java_com_irdeto_drm_ChinaDrm_native_initIDs(JNIEnv* env, jobject thiz) {
    jclass cls = env->FindClass( CLASS_NAME);
    MID_ERROR_NOTIFICATION = env->GetMethodID( cls, "errorNotification", "(IIILjava/lang/String;)V");
    MID_DRM_AGENT_REQUEST = env->GetMethodID( cls, "drmAgentRequest", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
    MID_DRM_REQUEST = env->GetMethodID( cls, "drmRequest", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
    LOGD("native init finished ... ");
    env->DeleteLocalRef( cls);
}

/**
 * Native for creating ChinaDRM.
 * Note: ac_drm_jacVerify should be invoked after ac_drm_initDrm.
 */
jint Java_com_irdeto_drm_ChinaDrm_native_startup(JNIEnv* env, jobject thiz, jstring securePath, jstring deviceInfo, jobject appCtx) {
    jint ret = -1;
    gContext.drm = env->NewGlobalRef( thiz);
    const char* pSecurePath = (securePath ? env->GetStringUTFChars( securePath, NULL) : NULL);
    const char* pDeviceInfo = (deviceInfo ? env->GetStringUTFChars( deviceInfo, NULL) : NULL);
    drm_callbacks callbacks = {errorNotification, drmAgentRequest, drmRequest};

    setCallBacks(&callbacks);
    LOGD("pSecurePath:%s", pSecurePath);
    LOGD("pDeviceInfo:%s", pDeviceInfo);

    ac_drm_android_env androidEnv = {NULL, NULL};
    androidEnv.javaVm = gContext.jvm;
    androidEnv.appContext = appCtx;
    if (initDrm(pSecurePath, pDeviceInfo, &androidEnv) == AC_DRM_SUCCESS) {
        ret = 0;
    }

    if (pSecurePath) env->ReleaseStringUTFChars(securePath, pSecurePath);
    if (pDeviceInfo) env->ReleaseStringUTFChars(deviceInfo, pDeviceInfo);

    return ret;
}

/**
 * Native for destroying ChinaDRM.
 */
jint Java_com_irdeto_drm_ChinaDrm_native_shutdown(JNIEnv* env, jobject thiz) {
    jint ret = -1;

    if (destoryDrm() == AC_DRM_SUCCESS) {
        ret = 0;
    }

    if (gContext.drm) {
        env->DeleteGlobalRef(gContext.drm);
        gContext.drm = NULL;
    }

    return ret;
}

/**
 * Native implement for acquiring license.
 */
jint Java_com_irdeto_drm_ChinaDrm_native_acquireLicense(JNIEnv* env, jobject thiz, jstring ecmData, jstring dataPath) {
    ac_drm_ret result = AC_DRM_SUCCESS;

    //ac_drm_setJacEnv((void *)env, (void *)thiz);
    //ac_drm_jacVerify();

    if (result == AC_DRM_SUCCESS) {
        ac_drm_byte_buffer drmMetadata = {0};
        ac_drm_byte_buffer createSessionUserDataBuf = {0};
        ac_drm_create_session_params createSessionParams = {0};
        jobject configInstance = getConfigInstance(env, thiz);
        jstring createSessionUserDataObj = getCreateSessionUserData(env, configInstance);
        const ac_drm_byte *pCreateSessionUserData = (const ac_drm_byte *)env->GetStringUTFChars(createSessionUserDataObj, NULL);

        drmMetadata.pData = (ecmData ? (ac_drm_byte *)env->GetStringUTFChars( ecmData, NULL) : NULL);
        drmMetadata.dataSize = (ecmData ? SPI_strlen((ac_drm_char *)drmMetadata.pData) + 1 : 0);

        createSessionUserDataBuf.pData = (ac_drm_byte *)pCreateSessionUserData;
        createSessionUserDataBuf.dataSize = strlen((const ac_drm_char *)pCreateSessionUserData) + 1;

        createSessionParams.metadataType = AC_DRM_METADATA_CHINADRM_HLS;
        createSessionParams.pDrmMetadata = &drmMetadata;
        createSessionParams.pUserData = (createSessionUserDataBuf.dataSize == 1) ? NULL : &createSessionUserDataBuf;

        LOGD("jni acquireLicense");
        result = acquireLicense(&createSessionParams);

        if (createSessionUserDataBuf.pData) env->ReleaseStringUTFChars( createSessionUserDataObj, (const char*)createSessionUserDataBuf.pData);
        if (drmMetadata.pData) env->ReleaseStringUTFChars( ecmData, (const char*)drmMetadata.pData);
    }

    return result;
}

/**
 * Native implement for acquiring license by URL.
 */
jint Java_com_irdeto_drm_ChinaDrm_native_acquireLicenseByUrl(JNIEnv* env, jobject thiz, jstring ecmData, jstring dataPath) {
    ac_drm_ret result = AC_DRM_SUCCESS;

    //ac_drm_setJacEnv((void *)env, (void *)thiz);
    //ac_drm_jacVerify();

    if (result == AC_DRM_SUCCESS) {
        ac_drm_byte_buffer drmMetadata = {0};
        ac_drm_byte_buffer createSessionUserDataBuf = {0};
        ac_drm_create_session_params createSessionParams = {0};
        jobject configInstance = getConfigInstance(env, thiz);
        //jstring createSessionUserDataObj = getCreateSessionUserData(env, configInstance);
        //const ac_drm_byte *pCreateSessionUserData = (*env)->GetStringUTFChars(env, createSessionUserDataObj, NULL);
        drmMetadata.pData = (ecmData ?(ac_drm_byte *) env->GetStringUTFChars( ecmData, NULL) : NULL);
        drmMetadata.dataSize = (ecmData ? SPI_strlen((ac_drm_char *)drmMetadata.pData) + 1 : 0);

        //createSessionUserDataBuf.pData = (ac_drm_byte *)pCreateSessionUserData;
       //createSessionUserDataBuf.dataSize = strlen((const ac_drm_char *)pCreateSessionUserData) + 1;

        createSessionParams.metadataType = AC_DRM_METADATA_CHINADRM_HLS_URL;
        createSessionParams.pDrmMetadata = &drmMetadata;
        createSessionParams.pUserData = (createSessionUserDataBuf.dataSize == 1) ? NULL : &createSessionUserDataBuf;

        LOGD("jni acquireLicense");
        result = acquireLicense(&createSessionParams);

        //if (createSessionUserDataBuf.pData) (*env)->ReleaseStringUTFChars(env, createSessionUserDataObj, createSessionUserDataBuf.pData);
        if (drmMetadata.pData) env->ReleaseStringUTFChars( ecmData, (const char*)drmMetadata.pData);
    }

    return result;
}

/**
 * Native implement for setting force quit.
 */
jint Java_com_irdeto_drm_ChinaDrm_native_setForceQuit(JNIEnv* env, jobject thiz) {
    ac_drm_ret result = AC_DRM_SUCCESS;

    //ac_drm_setJacEnv((void *)env, (void *)thiz);
    //ac_drm_jacVerify();

    if (result == AC_DRM_SUCCESS) {
        LOGD("jni setForceQuit");
        result = setForceQuit();
    }

    return result;
}

/**
 * Native implement for playing protected content.
 */
jint Java_com_irdeto_drm_ChinaDrm_native_play(JNIEnv* env, jobject thiz, jstring ecmData, jstring dataFile) {
    ac_drm_ret result = AC_DRM_SUCCESS;
    ac_drm_session_handle handle = NULL;

    //ac_drm_setJacEnv((void *)env, (void *)thiz);
    //ac_drm_jacVerify();


    if (result == AC_DRM_SUCCESS) {
        const ac_drm_char* pDataFile = (dataFile ? env->GetStringUTFChars(dataFile, NULL) : NULL);
        ac_drm_byte_buffer drmMetadata = {0};
        ac_drm_byte_buffer createSessionUserDataBuf = {0};
        ac_drm_create_session_params createSessionParams = {0};
        jobject configInstance = getConfigInstance(env, thiz);
        jstring createSessionUserDataObj = getCreateSessionUserData(env, configInstance);
        const ac_drm_byte *pCreateSessionUserData = ( const ac_drm_byte *)env->GetStringUTFChars( createSessionUserDataObj, NULL);

        drmMetadata.pData = (ecmData ? ( ac_drm_byte *)env->GetStringUTFChars( ecmData, NULL) : NULL);
        drmMetadata.dataSize = (ecmData ? SPI_strlen((ac_drm_char *)drmMetadata.pData) + 1 : 0);

        createSessionUserDataBuf.pData = (ac_drm_byte *)pCreateSessionUserData;
        createSessionUserDataBuf.dataSize = strlen((const ac_drm_char *)pCreateSessionUserData) + 1;

        createSessionParams.metadataType = AC_DRM_METADATA_CHINADRM_HLS;
        createSessionParams.pDrmMetadata = &drmMetadata;
        createSessionParams.pUserData = (createSessionUserDataBuf.dataSize == 1) ? NULL : &createSessionUserDataBuf;

        result = createSession(&createSessionParams, &handle, AC_DRM_FALSE);
        if (result == AC_DRM_SUCCESS) {
            LOGD("play NO.1 segment");
            result = play(handle, pDataFile);
        }

        if (result == AC_DRM_SUCCESS) {
            LOGD("play NO.2 segment");
            result = updateSession(handle, &drmMetadata, AC_DRM_METADATA_CHINADRM_HLS);
            if (result == AC_DRM_SUCCESS) {
                result = play(handle, pDataFile);
            }
        }

        if (handle != NULL) {
            destroySession(handle);
        }

        if (createSessionUserDataBuf.pData) env->ReleaseStringUTFChars( createSessionUserDataObj, (const char*)createSessionUserDataBuf.pData);
        if (drmMetadata.pData) env->ReleaseStringUTFChars( ecmData, (const char *)drmMetadata.pData);
        if (dataFile) env->ReleaseStringUTFChars( dataFile, pDataFile);
    }

    return result;
}

/**
 * Native implement for playing protected content with local license.
 */
jint Java_com_irdeto_drm_ChinaDrm_native_play_local(JNIEnv* env, jobject thiz, jstring ecmData, jstring dataFile) {
    ac_drm_ret result = AC_DRM_SUCCESS;
    ac_drm_session_handle handle = NULL;

   // ac_drm_setJacEnv((void *)env, (void *)thiz);
    //ac_drm_jacVerify();


    if (result == AC_DRM_SUCCESS) {
        const ac_drm_char* pDataFile = (dataFile ? env->GetStringUTFChars( dataFile, NULL) : NULL);
        ac_drm_byte_buffer drmMetadata = {0};
        ac_drm_byte_buffer createSessionUserDataBuf = {0};
        ac_drm_create_session_params createSessionParams = {0};
        jobject configInstance = getConfigInstance(env, thiz);
        jstring createSessionUserDataObj = getCreateSessionUserData(env, configInstance);
        const ac_drm_byte *pCreateSessionUserData = (ac_drm_byte *)env->GetStringUTFChars( createSessionUserDataObj, NULL);

        drmMetadata.pData = (ecmData ? (ac_drm_byte *)env->GetStringUTFChars( ecmData, NULL) : NULL);
        drmMetadata.dataSize = (ecmData ? SPI_strlen((ac_drm_char *)drmMetadata.pData) + 1 : 0);

        createSessionUserDataBuf.pData = (ac_drm_byte *)pCreateSessionUserData;
        createSessionUserDataBuf.dataSize = strlen((const ac_drm_char *)pCreateSessionUserData) + 1;

        createSessionParams.metadataType = AC_DRM_METADATA_CHINADRM_HLS;
        createSessionParams.pDrmMetadata = &drmMetadata;
        createSessionParams.pUserData = (createSessionUserDataBuf.dataSize == 1) ? NULL : &createSessionUserDataBuf;

        result = createSession(&createSessionParams, &handle, AC_DRM_TRUE);
        if (result == AC_DRM_SUCCESS) {
            LOGD("play NO.1 segment");
            result = play(handle, pDataFile);
        }

        if (result == AC_DRM_SUCCESS) {
            LOGD("play NO.2 segment");
            result = updateSession(handle, &drmMetadata, AC_DRM_METADATA_CHINADRM_HLS);
            if (result == AC_DRM_SUCCESS) {
                result = play(handle, pDataFile);
            }
        }

        if (handle != NULL) {
            destroySession(handle);
        }

        if (createSessionUserDataBuf.pData) env->ReleaseStringUTFChars( createSessionUserDataObj, (const char *)createSessionUserDataBuf.pData);
        if (drmMetadata.pData) env->ReleaseStringUTFChars(ecmData, (const char *)drmMetadata.pData);
        if (dataFile) env->ReleaseStringUTFChars( dataFile, pDataFile);
    }

    return result;
}


jstring Java_com_irdeto_drm_ChinaDrm_native_queryinfo(JNIEnv* env, jobject thiz, jstring ecmData) {
    LOGD("query info {");

    ac_drm_ret result = AC_DRM_SUCCESS;
    ac_drm_session_handle handle = NULL;
    jstring strInfo = NULL;
   // ac_drm_setJacEnv((void *)env, (void *)thiz);
    //ac_drm_jacVerify();

    if (result == AC_DRM_SUCCESS) {
        ac_drm_byte_buffer drmMetadata = {0};
        ac_drm_byte_buffer createSessionUserDataBuf = {0};
        ac_drm_create_session_params createSessionParams = {0};
        jobject configInstance = getConfigInstance(env, thiz);
        jstring createSessionUserDataObj = getCreateSessionUserData(env, configInstance);
        const ac_drm_byte *pCreateSessionUserData = (ac_drm_byte *)env->GetStringUTFChars( createSessionUserDataObj, NULL);

        drmMetadata.pData = (ecmData ? (ac_drm_byte *)env->GetStringUTFChars( ecmData, NULL) : NULL);
        drmMetadata.dataSize = (ecmData ? SPI_strlen((ac_drm_char *)drmMetadata.pData) + 1 : 0);

        createSessionUserDataBuf.pData = (ac_drm_byte *)pCreateSessionUserData;
        createSessionUserDataBuf.dataSize = strlen((const ac_drm_char *)pCreateSessionUserData) + 1;

        createSessionParams.metadataType = AC_DRM_METADATA_CHINADRM_HLS;
        createSessionParams.pDrmMetadata = &drmMetadata;
        createSessionParams.pUserData = (createSessionUserDataBuf.dataSize == 1) ? NULL : &createSessionUserDataBuf;

        result = createSession(&createSessionParams, &handle, AC_DRM_FALSE);

        if (result == AC_DRM_SUCCESS) {
            const ac_drm_char* info = queryinfo(handle);
            strInfo = newStringFromStandardUTF8(env, (ac_drm_char *)info);
        }

        if (handle != NULL) {
            destroySession(handle);
        }

        if (createSessionUserDataBuf.pData) env->ReleaseStringUTFChars( createSessionUserDataObj, (const char*)createSessionUserDataBuf.pData);
        if (drmMetadata.pData) env->ReleaseStringUTFChars( ecmData, (const char *)drmMetadata.pData);
    }

    LOGD("query info }");
    return strInfo;
}

static ac_drm_ret decrypt(ac_drm_session_handle handle, ac_drm_byte* arr, ac_drm_uint32 length, jboolean isFinal, int* puffer_decrypted_size )
{
    ac_drm_decryptParameter decParameter = {0};
    //memcpy(decParameter.iv, gDecryptParameter.iv, 16);
    if(isFinal){
        decParameter.final = AC_DRM_TRUE;
    }
    else{
        decParameter.final = AC_DRM_FALSE;
    }

    ac_drm_ret ret = AC_DRM_SUCCESS;

    ac_drm_byte_buffer buffer = {0};

    if(length > 0){
        //LOGD("read... %d", length);
        // decrypt data
        //LOGD("decryptBuffer {");
        buffer.pData = arr;
        buffer.dataSize = length;

        ret = decryptBuffer(handle, &buffer, &decParameter);
        //memcpy(gDecryptParameter.iv, decParameter.iv,16);
        //LOGD("decryptBuffer } result = %d", ret);
        /*if(buffer.dataSize!= length)
            ret = AC_DRM_UNKNOWN_ERROR;*/
        *puffer_decrypted_size = buffer.dataSize;
    }
    return ret;
}

void Java_com_irdeto_ChinaDrm_destorySession(JNIEnv* env, jobject thiz){
    ac_drm_ret result = AC_DRM_SUCCESS;

    if (gSessionHandle != NULL) {
        destroySession(gSessionHandle);
    }
    gSessionHandle = NULL;

}


static ac_drm_ret createSessionHandle(JNIEnv* env, jobject thiz, jstring ecmData){
    ac_drm_ret result = AC_DRM_SUCCESS;
    ac_drm_byte_buffer drmMetadata = {0};
    ac_drm_byte_buffer createSessionUserDataBuf = {0};
    ac_drm_create_session_params createSessionParams = {0};

    jobject configInstance = getConfigInstance(env, thiz);
    jstring createSessionUserDataObj = getCreateSessionUserData(env, configInstance);
    const ac_drm_byte *pCreateSessionUserData = (const ac_drm_byte *)env->GetStringUTFChars(createSessionUserDataObj, NULL);
    drmMetadata.pData = (ecmData ? (ac_drm_byte *)env->GetStringUTFChars(ecmData, NULL) : NULL);
    drmMetadata.dataSize = (ecmData ? SPI_strlen((const ac_drm_char *)drmMetadata.pData) + 1 : 0);

    createSessionUserDataBuf.pData = (ac_drm_byte *)pCreateSessionUserData;
    createSessionUserDataBuf.dataSize = strlen((const ac_drm_char *)pCreateSessionUserData) + 1;

    createSessionParams.metadataType = AC_DRM_METADATA_CHINADRM_HLS;
    createSessionParams.pDrmMetadata = &drmMetadata;
    createSessionParams.pUserData = (createSessionUserDataBuf.dataSize == 1) ? NULL : &createSessionUserDataBuf;

    result = createSession(&createSessionParams, &gSessionHandle, AC_DRM_FALSE);

    if (createSessionUserDataBuf.pData) env->ReleaseStringUTFChars( createSessionUserDataObj, (ac_drm_char *)createSessionUserDataBuf.pData);
    if (drmMetadata.pData) env->ReleaseStringUTFChars( ecmData, (ac_drm_char *)drmMetadata.pData);
    return result;
}

void Java_com_irdeto_ChinaDrm_createSession(JNIEnv* env, jobject thiz, jstring ecmData){

    createSessionHandle(env,thiz,ecmData);
}

jbyteArray Java_com_irdeto_drm_ChinaDrm_decryptBuffer(JNIEnv* env, jobject thiz, jbyteArray buffer, jstring ecmData, jboolean isFinal){
    //LOGD("decryptBuffer");
    ac_drm_uint32 len = 0;
    ac_drm_ret ret = AC_DRM_SUCCESS;
    int decrypt_size = 0;

    ac_drm_byte_buffer drmMetadata = {0};
    drmMetadata.pData = (ecmData ? (ac_drm_byte *)env->GetStringUTFChars(ecmData, NULL) : NULL);
    drmMetadata.dataSize = (ecmData ? SPI_strlen((const ac_drm_char *)drmMetadata.pData) + 1 : 0);

    if(gSessionHandle == NULL){
       // drmMetadata.pData = (ecmData ? (ac_drm_byte *)env->GetStringUTFChars(ecmData, NULL) : NULL);
        //drmMetadata.dataSize = (ecmData ? SPI_strlen((const ac_drm_char *)drmMetadata.pData) + 1 : 0);
        if(createSessionHandle(env, thiz, ecmData) != AC_DRM_SUCCESS)
            return NULL;
    } else{
       // ret = updateSession(gSessionHandle, &drmMetadata, AC_DRM_METADATA_CHINADRM_HLS);
    }
    if(ret != AC_DRM_SUCCESS) {
        return NULL;
    }
    jbyte *c_array =env->GetByteArrayElements(buffer, 0);
    int len_arr = env->GetArrayLength(buffer);
    ret = decrypt(gSessionHandle,(ac_drm_byte *)c_array, len_arr,isFinal,&decrypt_size);
    if(ret != AC_DRM_SUCCESS) {
        env->ReleaseByteArrayElements(buffer, c_array, 0);
        if (drmMetadata.pData) env->ReleaseStringUTFChars( ecmData, (ac_drm_char *)drmMetadata.pData);
        return NULL;
    }

    if(isFinal){
        ret = updateSession(gSessionHandle, &drmMetadata, AC_DRM_METADATA_CHINADRM_HLS);
    }
    jbyteArray c_result = env->NewByteArray(decrypt_size);

    env->SetByteArrayRegion(c_result, 0, decrypt_size, c_array);
    env->ReleaseByteArrayElements(buffer, c_array, 0);
    //env->DeleteLocalRef(c_result);
    return c_result;

}

static JNINativeMethod g_methods[] = {
        { _NATIVE_INITIDS, "()V", (void*) Java_com_irdeto_drm_ChinaDrm_native_initIDs },
        { _NATIVE_STARTUP, "(Ljava/lang/String;Ljava/lang/String;Landroid/content/Context;)I", (void*) Java_com_irdeto_drm_ChinaDrm_native_startup },
        { _NATIVE_SHUTDOWN, "()I", (void*) Java_com_irdeto_drm_ChinaDrm_native_shutdown },
        { _NATIVE_PLAY, "(Ljava/lang/String;Ljava/lang/String;)I", (void*) Java_com_irdeto_drm_ChinaDrm_native_play },
        { _NATIVE_PLAY_LOCAL, "(Ljava/lang/String;Ljava/lang/String;)I", (void*) Java_com_irdeto_drm_ChinaDrm_native_play_local },
        { _NATIVE_QUERY_INFO, "(Ljava/lang/String;)Ljava/lang/String;", (void*) Java_com_irdeto_drm_ChinaDrm_native_queryinfo },
        { _NATIVE_ACQUIRELICENSE, "(Ljava/lang/String;Ljava/lang/String;)I", (void*) Java_com_irdeto_drm_ChinaDrm_native_acquireLicense },
        { _NATIVE_ACQUIRELICENSEBYURL, "(Ljava/lang/String;Ljava/lang/String;)I", (void*) Java_com_irdeto_drm_ChinaDrm_native_acquireLicenseByUrl },
        {_NATIVE_DECRYPTBUFFER,"([BLjava/lang/String;Z)[B",(void *) Java_com_irdeto_drm_ChinaDrm_decryptBuffer},
        {_NATIVE_CREATE_SESSION,"(Ljava/lang/String;)V",(void *) Java_com_irdeto_ChinaDrm_createSession},
        {_NATIVE_DESTORY_SESSION,"()V",(void *)Java_com_irdeto_ChinaDrm_destorySession},
        { _NATIVE_SETFORCEQUIT, "()I", (void*) Java_com_irdeto_drm_ChinaDrm_native_setForceQuit }
};

/**
 * Register ChinaDrm native methods.
 */
jint register_com_irdeto_drm_ChinaDrm(JavaVM* jvm) {
    jint result = -1;
    JNIEnv* env = NULL;
    gContext.jvm = jvm;

    if (jvm->GetEnv((void **)&env, JNI_VERSION_1_4) == JNI_OK) {
        jclass clazz = env->FindClass( CLASS_NAME);

        if (clazz != NULL) {
            if (env->RegisterNatives(clazz, g_methods, sizeof(g_methods) / sizeof(JNINativeMethod)) == 0) {
                result = JNI_VERSION_1_4;
            }

            env->DeleteLocalRef( clazz);
        }
    }

    return result;
}


/**
 * Unregister ChinaDrm native methods.
 */
void unregister_com_irdeto_drm_ChinaDrm(JavaVM* jvm) {
    JNIEnv* env = NULL;
    if (jvm->GetEnv((void **)&env, JNI_VERSION_1_4) == JNI_OK) {
        jclass clazz = env->FindClass(CLASS_NAME);
        if (clazz != NULL) {
            env->UnregisterNatives(clazz);
            env->DeleteLocalRef( clazz);
        }
    }
}

jint JNI_OnLoad(JavaVM* jvm, void* reserved) {
    return register_com_irdeto_drm_ChinaDrm(jvm);
}

void JNI_OnUnload(JavaVM* jvm, void* reserved) {
    // To enable JAC, chinadrm must be destroyed in JNI_OnUnload()
    destoryDrm();
    ac_drm_ShutDown();

    unregister_com_irdeto_drm_ChinaDrm(jvm);
}
