package com.tencent.cloud.polaris.config.configdata;

import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataResource;
import org.springframework.boot.context.config.Profiles;

import java.util.Objects;

/**
 * A polaris configData resource from which {@link ConfigData} can be loaded.
 *
 * @author wlx
 * @date 2022/7/5 11:13 下午
 */
public class PolarisConfigDataResource extends ConfigDataResource {

	private final PolarisConfigProperties polarisConfigProperties;

	private final PolarisContextProperties polarisContextProperties;

	private final Profiles profiles;

	private final boolean optional;

	private final String fileName;

	private final String serviceName;

	public PolarisConfigDataResource(PolarisConfigProperties polarisConfigProperties,
									 PolarisContextProperties polarisContextProperties,
									 Profiles profiles,
									 boolean optional,
									 String fileName,
									 String serviceName) {
		this.polarisConfigProperties = polarisConfigProperties;
		this.polarisContextProperties = polarisContextProperties;
		this.profiles = profiles;
		this.optional = optional;
		this.fileName = fileName;
		this.serviceName = serviceName;
	}

	public PolarisConfigProperties getPolarisConfigProperties() {
		return polarisConfigProperties;
	}

	public PolarisContextProperties getPolarisContextProperties() {
		return polarisContextProperties;
	}

	public Profiles getProfiles() {
		return profiles;
	}

	public boolean isOptional() {
		return optional;
	}

	public String getFileName() {
		return fileName;
	}

	public String getServiceName() {
		return serviceName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PolarisConfigDataResource that = (PolarisConfigDataResource) o;
		return optional == that.optional &&
				polarisConfigProperties.equals(that.polarisConfigProperties) &&
				polarisContextProperties.equals(that.polarisContextProperties) &&
				profiles.equals(that.profiles) &&
				fileName.equals(that.fileName) &&
				serviceName.equals(that.serviceName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(polarisConfigProperties, polarisContextProperties, profiles, optional, fileName, serviceName);
	}

	@Override
	public String toString() {
		return "PolarisConfigDataResource{" +
				"polarisConfigProperties=" + polarisConfigProperties +
				", polarisContextProperties=" + polarisContextProperties +
				", profiles=" + profiles +
				", optional=" + optional +
				", fileName='" + fileName + '\'' +
				", serviceName='" + serviceName + '\'' +
				'}';
	}
}
