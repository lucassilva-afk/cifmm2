package br.com.cifmm.util;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.util.Map;

public class CloudinaryConfig {
    public static Cloudinary getCloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "SEU_CLOUD_NAME",
                "api_key", "SUA_API_KEY",
                "api_secret", "SEU_API_SECRET"));
    }
}
