/**
 * @file drm.c
 *
 * Implement all ChinaDRM CallBacks, and show some procedure sample.
 *
 */

#include "ac_drm_core.h"
#include "spi.h"
#include "drm.h"
#include "stdio.h"
#include "string.h"
#include "unistd.h"

#define FILENAME_CDRM_ASSET "cdrm_asset.dat"
static ac_drm_handle drmHandle;
static drm_callbacks m_callbacks;

static ac_drm_bool sessionCreated = AC_DRM_FALSE;
static ac_drm_session_handle sessionHandle = NULL;

#define TAG "drm"


/**
 * Structure for user data.
 *
 */
typedef struct {
    void* pContext;     // DRM context.
    ac_drm_sem_handle sem;     // a semaphore which will be used while acquiring license.
    ac_drm_int status;  // status of DRM session.
} ac_drm_user_data;

static ac_drm_user_data userData = {0};

/**
 * Implement drmLicenseReady defined in ac_drm_core.h
 *
 */
static ac_drm_bool drmLicenseReady(
		ac_drm_session_handle sessionHandle,
		const ac_drm_char *licensePolicy,
		void* pUserData)
{
	ac_drm_user_data *pPrivUserData = (ac_drm_user_data *) pUserData;
	pPrivUserData->status = AC_DRM_SUCCESS;
	SPI_Sem_Post(pPrivUserData->sem);
}

/**
 * Implement errorNotification defined in ac_drm_core.h
 *
 */
static void errorNotification(
    ac_drm_error_severity severity,
    ac_drm_ret errorCode,
    ac_drm_int additionalCode,
    const ac_drm_char *pDescription,
    void *pUserData)
{
    ac_drm_user_data* pPrivUserData = (ac_drm_user_data*) pUserData;
	if (m_callbacks.errorNotification != NULL) {
	    drmErrorMsg msg = {0};
	    msg.severity = severity;
	    msg.errorCode = errorCode;
	    msg.additionalCode = additionalCode;
	    msg.description = (ac_drm_char *)pDescription;
	    msg.pUserData = pPrivUserData->pContext;
		m_callbacks.errorNotification(&msg);
	}
}

/**
 * Implement drmAgentRequest defined in ac_drm_core.h
 *
 */
static ac_drm_bool drmAgentRequest(
		ac_drm_session_handle sessionHandle,
		const ac_drm_request_params *pRequestParams,
		void* pUserData)
{
	ac_drm_uint32 status = 0;
	ac_drm_uint32 ret = AC_DRM_SUCCESS;
	ac_drm_bool retVal = AC_DRM_FALSE;
	ac_drm_user_data *pPrivUserData = (ac_drm_user_data *) pUserData;
	ac_drm_byte_buffer pResponseData={0};

	// check parameters
	if (sessionHandle == NULL || pRequestParams == NULL || pRequestParams->pUrl == NULL)
		return retVal;

	// net perform
	if (m_callbacks.drmAgentRequest != NULL) {
		drmRequestMsg msg = {0};
		msg.pUrl = (ac_drm_char *)pRequestParams->pUrl;
		msg.type = pRequestParams->type;
		msg.pPost = pRequestParams->pRequestData;
		msg.pStatus = &status;
		msg.pResBody = &pResponseData;
		msg.pUserData = pPrivUserData->pContext;

		if (m_callbacks.drmAgentRequest(&msg) == AC_DRM_FALSE) {
			ret = AC_DRM_UNKNOWN_ERROR;
		}
	} else {
		ret = AC_DRM_UNKNOWN_ERROR;
	}

	if (ret == AC_DRM_SUCCESS) {
		ret = ac_drm_processResponseData(sessionHandle, &pResponseData);
	}

	SPI_free(pResponseData.pData);
	if (ret != AC_DRM_SUCCESS) {
		pPrivUserData->status = AC_DRM_UNKNOWN_ERROR;
		SPI_Sem_Post(pPrivUserData->sem);
		retVal = AC_DRM_FALSE;
	} else {
		retVal = AC_DRM_TRUE;
	}

	LOGD("retVal:%d", ret);
	return retVal;
}

/**
 * Implement drmRequest defined in ac_drm_core.h
 *
 */
static ac_drm_bool drmRequest(
		void* pRequestHandle,
		const ac_drm_request_params *pRequestParams,
		void *pUserData)
{
	ac_drm_uint32 ret = AC_DRM_SUCCESS;
	ac_drm_bool retVal = AC_DRM_FALSE;
	ac_drm_byte_buffer pResponseData={0};

	// check parameters
	if (pRequestHandle == NULL || pRequestParams == NULL || pRequestParams->pUrl == NULL)
		return retVal;

	// net perform
	if (m_callbacks.drmRequest != NULL) {
		drmRequestMsg msg = {0};
		msg.pUrl = (ac_drm_char *)pRequestParams->pUrl;
		msg.type = pRequestParams->type;
		msg.pPost = pRequestParams->pRequestData;
		msg.pResBody = &pResponseData;

		if (m_callbacks.drmRequest(&msg) == AC_DRM_FALSE) {
			ret = AC_DRM_UNKNOWN_ERROR;
		}
	} else {
		ret = AC_DRM_UNKNOWN_ERROR;
	}

	if (ret == AC_DRM_SUCCESS) {
		ret = ac_drm_processDrmResponse(pRequestHandle, &pResponseData);
	}
	LOGD("pResponseData : %s", pResponseData.pData);
	SPI_free(pResponseData.pData);
	if (ret != AC_DRM_SUCCESS) {
		retVal = AC_DRM_FALSE;
	} else {
		retVal = AC_DRM_TRUE;
	}

	LOGD("retVal:%d", ret);
	return retVal;
}

/**
 * To initialize ChinaDRM.
 */
ac_drm_ret initDrm(const ac_drm_char* pSecurePath, const ac_drm_char* pUserData, ac_drm_android_env* pAndroidEnv) {
	LOGD("initDrm {");
	ac_drm_ret result = AC_DRM_SUCCESS;
	ac_drm_handle handle = NULL;
	ac_drm_bool isRooted = AC_DRM_FALSE;
	ac_drm_init_params initParams = {0};
	ac_drm_callbacks callbacks = {0};
	ac_drm_byte_buffer version = {0};
	if (ac_drm_getVersionInfo(&version) == AC_DRM_SUCCESS)
	{
		LOGD("Agent Version:%s", version.pData);
	}
	ac_drm_deleteBuffer(&version);

	if ((pSecurePath == NULL) || (pUserData == NULL)) {
		result = AC_DRM_UNKNOWN_ERROR;
	}

	if (drmHandle == NULL) {
		callbacks.drmRequest = drmRequest;
		initParams.pContext = NULL;
		initParams.pAndroidEnv = pAndroidEnv;
		initParams.drmType = AC_DRM_TYPE_CHINADRM;
		initParams.pDataPath = (ac_drm_char *)pSecurePath;
		initParams.pDrmParams = (void *)pUserData;
        LOGD("pSecurePath is :%s", pSecurePath);
		result = ac_drm_initDrm(&handle, &initParams, &callbacks, (void *)pUserData);
		if (result == AC_DRM_SUCCESS) {
			drmHandle = handle;
		}
	}
//todo: temp disable below code for debug
/*	if (result == AC_DRM_SUCCESS) {
		result = ac_drm_getRootStatus(&isRooted);
		if (isRooted == AC_DRM_TRUE) {
			errorNotification(500, 200, 200, "device is rooted", &userData);
		}
	}*/

	LOGD("initDrm } result=%x", result);
	return result;
}

/**
 * To destroy ChinaDRM
 */
ac_drm_ret destoryDrm() {
	ac_drm_ret result = AC_DRM_SUCCESS;
	LOGD("destoryDrm {");

	if (drmHandle) {
		result = ac_drm_destroyDrm(drmHandle);
		drmHandle = NULL;
	}

	LOGD("destoryDrm } result:%d", result);
	return result;
}

/**
 * To set call backs implemented by upper layer.
 */
void setCallBacks(drm_callbacks* pCallbacks) {
	if (pCallbacks != NULL) {
		m_callbacks.errorNotification = pCallbacks->errorNotification;
		m_callbacks.drmAgentRequest = pCallbacks->drmAgentRequest;
		m_callbacks.drmRequest = pCallbacks->drmRequest;
	}
}

/**
 * To acquire license for specific content
 */
ac_drm_ret acquireLicense(ac_drm_create_session_params *pCreateSessionParams) {
	ac_drm_ret result = AC_DRM_SUCCESS;
	ac_drm_session_handle handle = NULL;

	ac_drm_user_data userData = {0};
	ac_drm_session_callbacks callbacks = {0};
	callbacks.errorNotification = errorNotification;
	callbacks.drmAgentRequest = drmAgentRequest;
	callbacks.drmLicenseReady = drmLicenseReady;

	userData.pContext = NULL;

	LOGD("pCreateSessionParams->pDrmMetadata->pData: %s", pCreateSessionParams->pDrmMetadata->pData);
    LOGD("pCreateSessionParams->pDrmMetadata: %x", pCreateSessionParams->pDrmMetadata);
    LOGD("pCreateSessionParams->metaDataType: %d", pCreateSessionParams->metadataType);
	LOGD("pCreateSessionParams->pUserData: %s", (pCreateSessionParams->pUserData == NULL) ? "NULL" : (ac_drm_char*)pCreateSessionParams->pUserData->pData);
	if (result == AC_DRM_SUCCESS) {
		result = ac_drm_createDrmSession(drmHandle, pCreateSessionParams, &callbacks, &userData, &handle);
		if (handle != NULL) {
			sessionCreated = AC_DRM_TRUE;
			sessionHandle = handle;
		}
		if (result == AC_DRM_LICENSE_NOT_EXIST
			|| result == AC_DRM_METADATA_DOWNLOAD_PENDING) {
			// stay here until license is ready.
			SPI_Sem_Init(&userData.sem, 0, 1);
			SPI_Sem_Wait(userData.sem);
			SPI_Sem_Destroy(&userData.sem);

			result = userData.status;
		}
	}

	if (handle != NULL) {
		ac_drm_destroyDrmSession(handle);
		sessionCreated = AC_DRM_FALSE;
		sessionHandle = NULL;
	}

	LOGD("acquireLicense: 0x%x", result);
	return result;
}


/**
 * To play content. createDrmSession
 */
ac_drm_ret createSession(ac_drm_create_session_params *pCreateSessionParams, ac_drm_session_handle *pHandle, ac_drm_bool disableAgentRequest) {
	ac_drm_ret result = AC_DRM_SUCCESS;
	ac_drm_session_handle handle = NULL;

	ac_drm_user_data userData = {0};
	ac_drm_session_callbacks callbacks = {0};
	callbacks.errorNotification = errorNotification;
	if (disableAgentRequest == AC_DRM_TRUE) {
		callbacks.drmAgentRequest = NULL;
	} else {
		callbacks.drmAgentRequest = drmAgentRequest;
	}
	callbacks.drmLicenseReady = drmLicenseReady;

	userData.pContext = NULL;

	if (drmHandle == NULL) {
		result = AC_DRM_UNKNOWN_ERROR;
	}

	LOGD("pCreateSessionParams->pDrmMetadata: %s", pCreateSessionParams->pDrmMetadata->pData);
	LOGD("pCreateSessionParams->pUserData: %s", (pCreateSessionParams->pUserData == NULL) ? "NULL" : (ac_drm_char*)pCreateSessionParams->pUserData->pData);
	if (result == AC_DRM_SUCCESS) {
		result = ac_drm_createDrmSession(drmHandle, pCreateSessionParams, &callbacks, &userData, &handle);
		if (result == AC_DRM_LICENSE_NOT_EXIST && !disableAgentRequest) {
			// stay here until license is ready.
			SPI_Sem_Init(&userData.sem, 0, 1);
			SPI_Sem_Wait(userData.sem);
			SPI_Sem_Destroy(&userData.sem);

			*pHandle = handle;
			result = userData.status;
		} else if (result == AC_DRM_SUCCESS) {
			*pHandle = handle;
		}
	}

	return result;
}

/**
 * To play content. updateDrmSession
 */
ac_drm_ret updateSession(ac_drm_session_handle handle, const ac_drm_byte_buffer* ecmData, ac_drm_metadata_type metadatType) {
	ac_drm_ret result = AC_DRM_SUCCESS;
	
	if (drmHandle == NULL || handle == NULL || ecmData == NULL) {
		result = AC_DRM_UNKNOWN_ERROR;
	}

	if (result == AC_DRM_SUCCESS) {
		ac_drm_update_session_params updateSessionParams = {
			metadatType,
			ecmData
		};
		result = ac_drm_updateDrmSession(handle, &updateSessionParams);
	}
	
	return result;
}

/**
 * destroySession
 */
ac_drm_ret destroySession(ac_drm_session_handle handle) {
	ac_drm_ret result = AC_DRM_SUCCESS;
	if (drmHandle == NULL) {
		result = AC_DRM_UNKNOWN_ERROR;
	}

	if (handle != NULL) {
		result = ac_drm_destroyDrmSession(handle);
		handle = NULL;
	}

	return result;
}

/**
 * Get DRM handle.
 */
DRMHANDLE getDrmHandle() {
	return (DRMHANDLE) drmHandle;
}


/**
 * Decrypt buffer.
 */
ac_drm_ret decryptBuffer(ac_drm_session_handle handle, ac_drm_byte_buffer *buffer, ac_drm_decryptParameter *decParameter) {
	ac_drm_ret ret = AC_DRM_SUCCESS;

	if (handle == NULL || decParameter == NULL) {
		LOGD("sessionHandle is NULL");
		ret = AC_DRM_UNKNOWN_ERROR;
	}

	if (ret == AC_DRM_SUCCESS) {
		ret = ac_drm_decryptBuffer(handle, buffer, NULL, decParameter);
	}

	return ret;
}

ac_drm_char* queryinfo(ac_drm_session_handle handle) {
	ac_drm_char tmp[1024] = {0};
	ac_drm_ret ret = AC_DRM_SUCCESS;
	ac_drm_queryInformation pQueryInfo = NULL;
	ac_drm_queryInformation_* pPrivQueryInfo = NULL;
	ac_drm_char* info = NULL;

	LOGD("query info {");
	if ((ret == AC_DRM_SUCCESS) && (handle != NULL)) {
		ret = ac_drm_queryInfo(handle, &pQueryInfo);
	}

	LOGD("query info result:%d", ret);
	if ((ret == AC_DRM_SUCCESS) && pQueryInfo != NULL) {
		pPrivQueryInfo = (ac_drm_queryInformation_*)pQueryInfo;
		if (pPrivQueryInfo->pContentID == NULL) {
			LOGD("can not get contentID");
		}

		LOGD("contentId:%s", pPrivQueryInfo->pContentID);

		sprintf( tmp,
				"contentId:%s,playPerTimes:%d,remainderTimes:%d,playByTimeLen:%d," \
				"remainderTimeLen:%d,playBytimeSections:%d,remainderTimeSections:%d," \
				"ConnectionProtectPlay:%d,playByQuality:%d,persist:%d,analogVideo:%d," \
				"airPlay:%d,miracast:%d"
				, pPrivQueryInfo->pContentID
				, pPrivQueryInfo->playPerTimes
				, pPrivQueryInfo->remainderTimes
				, pPrivQueryInfo->playByTimeLen
				, pPrivQueryInfo->remainderTimeLen
				, pPrivQueryInfo->playBytimeSections
				, pPrivQueryInfo->remainderTimeSections
				, pPrivQueryInfo->ConnectionProtectPlay
				, pPrivQueryInfo->playByQuality
				, pPrivQueryInfo->persist
				, pPrivQueryInfo->analogVideo
				, pPrivQueryInfo->airPlay
				, pPrivQueryInfo->miracast
				);
		info = (char *)SPI_malloc(strlen(tmp));
		sprintf(info, "%s", tmp);
	}
	
	SPI_free(pQueryInfo);

	LOGD("query info }");
	return info;
}

/**
 * To force quit from session process
 */
ac_drm_ret setForceQuit() {
	ac_drm_ret result = AC_DRM_SUCCESS;
	LOGD("setForceQuit {");

	while(sessionCreated == AC_DRM_FALSE) {
		sleep(1);
	}

	LOGD("sessionHandle:%x in setForcequit", sessionHandle);
	if (sessionCreated == AC_DRM_TRUE) {
		result = ac_drm_setForceQuit(sessionHandle);
	}
	LOGD("setForceQuit } result:%d", result);

	return result;
}

