// Save as "HelloJNI.c"
#include <jni.h>        // JNI header provided by JDK
#include <stdio.h>      // C Standard IO Header
#include <time.h>
#include "net_floodlightcontroller_learningswitch_NativeClock.h"   // Generated
 

long timespec_to_long_us(struct timespec t1)
{
    return (t1.tv_sec + t1.tv_nsec / 1000000000.0) * 1000000;
}

// Implementation of the native method sayHello()
JNIEXPORT jlong JNICALL Java_net_floodlightcontroller_learningswitch_NativeClock_getCurrentTimeMicros(JNIEnv *env, jobject thisObj) {
   struct timespec currentTime;
   clock_gettime(CLOCK_REALTIME, &currentTime);
   return timespec_to_long_us(currentTime);
}