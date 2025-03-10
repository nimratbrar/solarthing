package me.retrodaredevil.solarthing.message.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import me.retrodaredevil.solarthing.message.MessageSender;
import me.retrodaredevil.solarthing.packets.Packet;
import me.retrodaredevil.solarthing.packets.collection.FragmentedPacketGroup;
import me.retrodaredevil.solarthing.solar.common.DualTemperature;

import java.time.Duration;

@JsonTypeName("temperature")
public class TemperatureEvent implements MessageEvent {
	private final boolean low;
	private final boolean displayFahrenheit;
	private final float thresholdCelsius;
	private final Duration timeout;
	private final TemperatureType temperatureType;

	private Long lastSendNanos = null;

	public TemperatureEvent(
			@JsonProperty("low") Boolean low,
			@JsonProperty("celsius") Float thresholdCelsius,
			@JsonProperty("fahrenheit") Float thresholdFahrenheit,
			@JsonProperty(value = "timeout", required = true) String timeoutDurationString,
			@JsonProperty("from") TemperatureType temperatureType) {
		this.low = low != null && low; // default to high temperature
		if (thresholdCelsius != null) {
			this.thresholdCelsius = thresholdCelsius;
			displayFahrenheit = false;
		} else if (thresholdFahrenheit != null) {
			this.thresholdCelsius = (thresholdFahrenheit - 32) / 1.8f;
			displayFahrenheit = true;
		} else {
			throw new IllegalArgumentException("Either celsius or fahrenheit must be defined!");
		}
		this.timeout = Duration.parse(timeoutDurationString);
		if (temperatureType == null) {
			this.temperatureType = TemperatureType.BATTERY;
		} else {
			this.temperatureType = temperatureType;
		}
	}

	@Override
	public void run(MessageSender sender, FragmentedPacketGroup previous, FragmentedPacketGroup current) {
		Long lastSendNanos = this.lastSendNanos;
		if (lastSendNanos != null) {
			if (System.nanoTime() - lastSendNanos < timeout.toNanos()) {
				return; // timeout has not passed yet
			}
		}
		for (Packet packet : current.getPackets()) {
			if (packet instanceof DualTemperature) {
				DualTemperature dualTemperature = (DualTemperature) packet;
				if (temperatureType == TemperatureType.BATTERY) {
					if (check(sender, dualTemperature.getBatteryTemperatureCelsius().floatValue())) {
						return;
					}
				} else if (temperatureType == TemperatureType.CONTROLLER) {
					if (check(sender, dualTemperature.getControllerTemperatureCelsius().floatValue())) {
						return;
					}
				} else { // either
					if (check(sender, dualTemperature.getBatteryTemperatureCelsius().floatValue()) || check(sender, dualTemperature.getControllerTemperatureCelsius().floatValue())) {
						return;
					}
				}
			}
		}
	}
	private String temperatureToString(float temperatureCelsius) {
		// these character escapes are for the fahrenheit and celsius degree symbols
		if (displayFahrenheit) {
			return (temperatureCelsius * 1.8 + 32) + "\u2109";
		}
		return temperatureCelsius + "\u2103";
	}
	private boolean check(MessageSender sender, float temperatureCelsius) {
		if (low) {
			if (temperatureCelsius <= thresholdCelsius) {
				sender.sendMessage("Low temperature! " + temperatureToString(temperatureCelsius));
				lastSendNanos = System.nanoTime();
				return true;
			}
		} else {
			if (temperatureCelsius >= thresholdCelsius) {
				sender.sendMessage("High temperature! " + temperatureToString(temperatureCelsius));
				lastSendNanos = System.nanoTime();
				return true;
			}
		}
		return false;
	}

	public enum TemperatureType {
		BATTERY,
		CONTROLLER,
		EITHER
	}
}
