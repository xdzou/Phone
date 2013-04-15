LOCAL_PATH:= $(call my-dir)

# Static library with some common classes for the phone apps.
# To use it add this line in your Android.mk
#  LOCAL_STATIC_JAVA_LIBRARIES := com.android.phone.common
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	src/com/android/phone/CallLogAsync.java \
	src/com/android/phone/HapticFeedback.java

LOCAL_MODULE := com.android.phone.common
include $(BUILD_STATIC_JAVA_LIBRARY)

# Build the Phone app which includes the emergency dialer. See Contacts
# for the 'other' dialer.
include $(CLEAR_VARS)

LOCAL_JAVA_LIBRARIES := telephony-common telephony-msim
LOCAL_JAVA_LIBRARIES += com.qrd.plugin.feature_query
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_SRC_FILES += \
        src/com/android/phone/EventLogTags.logtags \
        src/com/android/phone/INetworkQueryService.aidl \
        src/com/android/phone/INetworkQueryServiceCallback.aidl \
        src/com/qualcomm/ims/IImsService.aidl \
        src/com/qualcomm/ims/IImsServiceListener.aidl

LOCAL_PACKAGE_NAME := Phone
LOCAL_CERTIFICATE := platform

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_PACKAGE)

# Build the test package
include $(call all-makefiles-under,$(LOCAL_PATH))
