package com.visibleautomation.verify;
public class RegistrationRequest {
    private String registrationId;
    private String publicKey;
    private String phoneNumber;
    public RegistrationRequest(String registrationId,
                               String publicKey,
                               String phoneNumber) {
        this.registrationId = registrationId;
        this.publicKey = publicKey;
        this.phoneNumber = phoneNumber;
    }
    public String getRegistrationId() {
        return registrationId;
    }
    public String getPublicKey() {
        return publicKey;
    }
    public String getPhoneNumber() {
        return phoneNumber;
    }
}
