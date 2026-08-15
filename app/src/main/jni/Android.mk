LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE    := rena
LOCAL_CFLAGS    := -w -s -Wno-error=format-security -fvisibility=hidden -fvisibility-inlines-hidden -fpermissive -fexceptions
LOCAL_CPPFLAGS  := -w -s -Wno-error=format-security -fvisibility=hidden -fvisibility-inlines-hidden -Werror -std=c++17 -Wno-error=c++11-narrowing -fpermissive -Wall -fexceptions
LOCAL_LDFLAGS   += -Wl,--gc-sections,--strip-all,-z,max-page-size=16384
LOCAL_LDLIBS    := -llog -landroid -ldl -lEGL -lGLESv2
LOCAL_ARM_MODE  := arm
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_SRC_FILES := native-lib.cpp
include $(BUILD_SHARED_LIBRARY)
