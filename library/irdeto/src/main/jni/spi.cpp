/**
 * Common functions needed while integrated chinadrm
 *
 *
 */
#include <semaphore.h>
#include <string.h>
#include "ac_drm_core.h"
#include "spi.h"
#include "stdio.h"
#include "errno.h"
#include "stdlib.h"

#define SEM_NAME_SIZE 64

/**
 * private semaphore handle structure
 */
typedef struct _ac_drm_sem_handle_private {
	sem_t* handle;
	ac_drm_char name[SEM_NAME_SIZE];
} ac_drm_sem_handle_private;

ac_drm_ret SPI_Sem_Init(ac_drm_sem_handle *pSemaphore, int initialValue, int maxValue) {
	ac_drm_ret result = AC_DRM_SUCCESS;
	static ac_drm_uint32 sem_index = 0;
	ac_drm_sem_handle_private* pPrivateSemaphore = NULL;

	if (!pSemaphore) {
		result = AC_DRM_UNKNOWN_ERROR;
	} else {
		pPrivateSemaphore = (ac_drm_sem_handle_private*) SPI_malloc(sizeof(ac_drm_sem_handle_private));
		if (pPrivateSemaphore) {
			pPrivateSemaphore->handle = (sem_t*) SPI_malloc(sizeof(sem_t));

			if (pPrivateSemaphore->handle) {
				int ret = sem_init(pPrivateSemaphore->handle, 0, initialValue);

				if (ret != 0) {
					result = AC_DRM_UNKNOWN_ERROR;
				}

				if (SEM_FAILED == pPrivateSemaphore->handle) {
					result = AC_DRM_UNKNOWN_ERROR;
				}
			} else {
				result = AC_DRM_UNKNOWN_ERROR;
			}
		}

		if (result != AC_DRM_SUCCESS) {
			SPI_free(pPrivateSemaphore);
			pPrivateSemaphore = NULL;
		}

		*pSemaphore = (ac_drm_sem_handle) pPrivateSemaphore;
	}
	return result;
}

ac_drm_ret SPI_Sem_Wait(ac_drm_sem_handle semaphore) {
	ac_drm_ret result = AC_DRM_SUCCESS;
	ac_drm_sem_handle_private* pPrivateSemaphore = (ac_drm_sem_handle_private*) semaphore;
	{
		if (pPrivateSemaphore == NULL || pPrivateSemaphore->handle == NULL || pPrivateSemaphore->handle == SEM_FAILED) {
			result = AC_DRM_UNKNOWN_ERROR;
		} else {
			int ret = 0;
			/* continue waiting as long as we keep getting interrupted by a signal */
			do {
				ret = sem_wait(pPrivateSemaphore->handle);

				if (ret == 0) {
					break;
				} else if (errno != EINTR) {
					break;
				}
			} while (1);
			if (0 != ret) {
				result = AC_DRM_UNKNOWN_ERROR;
			}
		}
	}

	return result;
}

/*---------------------------------------------------------------------------*/
ac_drm_ret SPI_Sem_TryWait(ac_drm_sem_handle semaphore) {
	ac_drm_ret result = AC_DRM_SUCCESS;
	ac_drm_sem_handle_private* pPrivateSemaphore = (ac_drm_sem_handle_private*) semaphore;
	//CDRM_TRACE(ZONE_MUTEX, "SPI_Sem_TryWait(0x%08X)", semaphore);

	if (pPrivateSemaphore == NULL || pPrivateSemaphore->handle == NULL || pPrivateSemaphore->handle == SEM_FAILED) {
		result = AC_DRM_UNKNOWN_ERROR;
	} else {
		if (0 != sem_trywait(pPrivateSemaphore->handle)) {
			result = AC_DRM_UNKNOWN_ERROR; /* DON'T log the error, it isn't fatal, in fact it's expected sometimes*/
		}
	}
	return result;
}

/*---------------------------------------------------------------------------*/
ac_drm_ret SPI_Sem_Post(ac_drm_sem_handle semaphore) {
	ac_drm_ret result = AC_DRM_SUCCESS;
	ac_drm_sem_handle_private* pPrivateSemaphore = (ac_drm_sem_handle_private*) semaphore;

	if (pPrivateSemaphore == NULL) {
		result = AC_DRM_UNKNOWN_ERROR;
	} else if (pPrivateSemaphore->handle == NULL) {
		result = AC_DRM_UNKNOWN_ERROR;
	} else if (pPrivateSemaphore->handle == SEM_FAILED) {
		result = AC_DRM_UNKNOWN_ERROR;
	} else {
		sem_post(pPrivateSemaphore->handle);
	}
	return result;
}

/*---------------------------------------------------------------------------*/
void SPI_Sem_Destroy(ac_drm_sem_handle *pSemaphore) {
	ac_drm_sem_handle_private* pPrivateSemaphore = (ac_drm_sem_handle_private*) (*pSemaphore);

	if (pSemaphore && *pSemaphore) {
		if (pPrivateSemaphore == NULL || pPrivateSemaphore->handle == NULL || pPrivateSemaphore->handle == SEM_FAILED) {
		} else {
			if (pPrivateSemaphore->handle) {
				SPI_free(pPrivateSemaphore->handle);
			}
		}

		SPI_free(pPrivateSemaphore);
		*pSemaphore = NULL;
	}
}

void *SPI_malloc(ac_drm_uint32 size) {
	 return (void *)malloc(size);
}

void SPI_free(void *ptr) {
	if (ptr != NULL)
		free(ptr);
}

void *SPI_memcpy(void *dest, const void *src, ac_drm_uint32 size) {
    return memcpy(dest, src, size);
}

ac_drm_uint32 SPI_strlen(const ac_drm_char *pString)
{
    ac_drm_uint32 len = 0;
    while(pString[len])
    {
        ++len;
    }
    return len;
}
