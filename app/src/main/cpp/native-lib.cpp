#include <jni.h>
#include <string>
#include <android/log.h>
#include <iostream>
#include <sstream>
#include <fstream>
#include <cstdlib>
#include <unistd.h>
#include <sys/wait.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "PocketCode", __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_pocketcode_MainActivity_compileAndRunC(JNIEnv *env, jobject thiz, jstring code)
{
    const char *sourceCode = env->GetStringUTFChars(code, 0);

    // Create temporary files
    std::string tempDir = "/data/data/com.example.pocketcode/cache/";
    std::string sourceFile = tempDir + "temp.c";
    std::string execFile = tempDir + "temp_exec";

    // Write source code to file
    std::ofstream file(sourceFile);
    file << sourceCode;
    file.close();

    // Compile with clang
    std::string compileCmd = "clang -o " + execFile + " " + sourceFile;

    std::ostringstream result;

    FILE *pipe = popen(compileCmd.c_str(), "r");
    if (!pipe)
    {
        result << "❌ Compilation failed: Could not execute compiler";
        env->ReleaseStringUTFChars(code, sourceCode);
        return env->NewStringUTF(result.str().c_str());
    }

    char buffer[128];
    std::string compileOutput;
    while (fgets(buffer, sizeof(buffer), pipe) != NULL)
    {
        compileOutput += buffer;
    }
    pclose(pipe);

    if (!compileOutput.empty())
    {
        result << "❌ Compilation Error:\n"
               << compileOutput;
        env->ReleaseStringUTFChars(code, sourceCode);
        return env->NewStringUTF(result.str().c_str());
    }

    // Execute the compiled program
    pipe = popen(execFile.c_str(), "r");
    if (!pipe)
    {
        result << "❌ Execution failed: Could not run compiled program";
        env->ReleaseStringUTFChars(code, sourceCode);
        return env->NewStringUTF(result.str().c_str());
    }

    result << "✅ Compilation successful\n=== Output ===\n";
    while (fgets(buffer, sizeof(buffer), pipe) != NULL)
    {
        result << buffer;
    }
    pclose(pipe);

    // Clean up
    remove(sourceFile.c_str());
    remove(execFile.c_str());

    env->ReleaseStringUTFChars(code, sourceCode);
    return env->NewStringUTF(result.str().c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_pocketcode_MainActivity_compileAndRunCpp(JNIEnv *env, jobject thiz, jstring code)
{
    const char *sourceCode = env->GetStringUTFChars(code, 0);

    // Create temporary files
    std::string tempDir = "/data/data/com.example.pocketcode/cache/";
    std::string sourceFile = tempDir + "temp.cpp";
    std::string execFile = tempDir + "temp_exec";

    // Write source code to file
    std::ofstream file(sourceFile);
    file << sourceCode;
    file.close();

    // Compile with clang++
    std::string compileCmd = "clang++ -std=c++17 -o " + execFile + " " + sourceFile;

    std::ostringstream result;

    FILE *pipe = popen(compileCmd.c_str(), "r");
    if (!pipe)
    {
        result << "❌ Compilation failed: Could not execute compiler";
        env->ReleaseStringUTFChars(code, sourceCode);
        return env->NewStringUTF(result.str().c_str());
    }

    char buffer[128];
    std::string compileOutput;
    while (fgets(buffer, sizeof(buffer), pipe) != NULL)
    {
        compileOutput += buffer;
    }
    pclose(pipe);

    if (!compileOutput.empty())
    {
        result << "❌ Compilation Error:\n"
               << compileOutput;
        env->ReleaseStringUTFChars(code, sourceCode);
        return env->NewStringUTF(result.str().c_str());
    }

    // Execute the compiled program
    pipe = popen(execFile.c_str(), "r");
    if (!pipe)
    {
        result << "❌ Execution failed: Could not run compiled program";
        env->ReleaseStringUTFChars(code, sourceCode);
        return env->NewStringUTF(result.str().c_str());
    }

    result << "✅ Compilation successful\n=== Output ===\n";
    while (fgets(buffer, sizeof(buffer), pipe) != NULL)
    {
        result << buffer;
    }
    pclose(pipe);

    // Clean up
    remove(sourceFile.c_str());
    remove(execFile.c_str());

    env->ReleaseStringUTFChars(code, sourceCode);
    return env->NewStringUTF(result.str().c_str());
}