package com.voxloud.provisioning.service;

import com.voxloud.provisioning.entity.Device;
import com.voxloud.provisioning.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ProvisioningServiceImpl implements ProvisioningService {

    @Value("${provisioning.domain}")
    private String domain;

    @Value("${provisioning.port}")
    private String port;

    @Value("${provisioning.codecs}")
    private String codecs;

    private DeviceRepository deviceRepository;

    public ProvisioningServiceImpl(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }


    public String getProvisioningFile(String macAddress) {
        Device device = deviceRepository.findByMacAddress(macAddress);
        if (device == null) return null;

        if (device.getModel() == Device.DeviceModel.CONFERENCE) {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append(String.format("  \"username\" : \"%s\",\n", device.getUsername()));
            json.append(String.format("  \"password\" : \"%s\",\n", device.getPassword()));

            String overrideFragment = device.getOverrideFragment();
            if (overrideFragment != null && !overrideFragment.isEmpty()) {
                // Remove braces from override fragment
                overrideFragment = overrideFragment.replaceAll("[{}]", "");
                // Split by comma and newline
                String[] overrides = overrideFragment.split("[,\n]");

                boolean domainOverridden = false;
                boolean portOverridden = false;

                for (String override : overrides) {
                    if (!override.trim().isEmpty()) {
                        String[] parts = override.split(":", 2);
                        String key = parts[0].trim().replace("\"", "");
                        String value = parts[1].trim().replace("\"", "");

                        if ("domain".equals(key)) {
                            json.append(String.format("  \"domain\" : \"%s\",\n", value));
                            domainOverridden = true;
                        } else if ("port".equals(key)) {
                            json.append(String.format("  \"port\" : \"%s\",\n", value));
                            portOverridden = true;
                        } else {
                            json.append(String.format("  \"%s\" : %s,\n", key, value));
                        }
                    }
                }

                if (!domainOverridden) {
                    json.append(String.format("  \"domain\" : \"%s\",\n", domain));
                }
                if (!portOverridden) {
                    json.append(String.format("  \"port\" : \"%s\",\n", port));
                }
            } else {
                json.append(String.format("  \"domain\" : \"%s\",\n", domain));
                json.append(String.format("  \"port\" : \"%s\",\n", port));
            }

            json.append(String.format("  \"codecs\" : [\"%s\"]\n", codecs.replace(",", "\",\"")));
            json.append("}");
            return json.toString();

        } else {
            // DESK phone
            StringBuilder props = new StringBuilder();
            props.append(String.format("username=%s\n", device.getUsername()));
            props.append(String.format("password=%s\n", device.getPassword()));

            String overrideFragment = device.getOverrideFragment();
            if (overrideFragment != null && !overrideFragment.isEmpty()) {
                // Split override fragment by newlines
                String[] overrides = overrideFragment.split("\n");

                boolean domainOverridden = false;
                boolean portOverridden = false;

                for (String override : overrides) {
                    if (!override.trim().isEmpty()) {
                        String[] parts = override.split("=", 2);
                        if ("domain".equals(parts[0].trim())) {
                            props.append(String.format("domain=%s\n", parts[1].trim()));
                            domainOverridden = true;
                        } else if ("port".equals(parts[0].trim())) {
                            props.append(String.format("port=%s\n", parts[1].trim()));
                            portOverridden = true;
                        } else {
                            props.append(String.format("%s=%s\n", parts[0].trim(), parts[1].trim()));
                        }
                    }
                }

                if (!domainOverridden) {
                    props.append(String.format("domain=%s\n", domain));
                }
                if (!portOverridden) {
                    props.append(String.format("port=%s\n", port));
                }
            } else {
                props.append(String.format("domain=%s\n", domain));
                props.append(String.format("port=%s\n", port));
            }

            props.append(String.format("codecs=%s", codecs));
            return props.toString();
        }
    }
}
