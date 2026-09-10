package com.zamazor.market.media.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.zamazor.market.media.ports.MediaStoragePort;
import com.zamazor.market.media.model.StoredMediaMetadata;
import com.zamazor.market.media.exception.MediaStorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class CloudinaryStorageAdapter implements MediaStoragePort {
	private final Cloudinary cloudinary;

	@Override
	public StoredMediaMetadata upload(InputStream inputStream, String fileName, String folderPath) {
		try {
			byte[] fileBytes = inputStream.readAllBytes();

			Map<?, ?> uploadResult = cloudinary.uploader().upload(fileBytes, ObjectUtils.asMap(
					"folder", "zamazor/%s".formatted(folderPath),
					"use_filename", true,
					"unique_filename", true,
					"overwrite", false
			));

			return new StoredMediaMetadata(
					uploadResult.get("public_id").toString(),
					uploadResult.get("secure_url").toString(),
					uploadResult.get("format").toString()
			);

		} catch (Exception e) {
			throw new MediaStorageException("Failed to upload image file: %s".formatted(fileName), e);
		}
	}

	@Override
	public void delete(String publicId) {
		try {
			cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
		} catch (Exception e) {
			throw new MediaStorageException("Failed to delete asset with public ID: %s".formatted(publicId), e);
		}
	}
}