
package com.example.artsgymapp_solo;

public interface FingerprintIdentificationCallback
{
    void onIdentificationResult(String memberId);
    void onIdentificationError(String errorMessage);
}
