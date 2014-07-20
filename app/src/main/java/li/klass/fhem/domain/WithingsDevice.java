package li.klass.fhem.domain;

import android.util.Log;

import org.w3c.dom.NamedNodeMap;

import java.util.List;

import li.klass.fhem.R;
import li.klass.fhem.appwidget.annotation.ResourceIdMapper;
import li.klass.fhem.domain.core.Device;
import li.klass.fhem.domain.core.DeviceChart;
import li.klass.fhem.domain.core.DeviceFunctionality;
import li.klass.fhem.domain.genericview.DetailViewSettings;
import li.klass.fhem.domain.genericview.ShowField;
import li.klass.fhem.util.ValueDescriptionUtil;

import static li.klass.fhem.service.graph.description.ChartSeriesDescription.getRegressionValuesInstance;
import static li.klass.fhem.service.graph.description.SeriesType.CO2;
import static li.klass.fhem.service.graph.description.SeriesType.FAT_RATIO;
import static li.klass.fhem.service.graph.description.SeriesType.TEMPERATURE;
import static li.klass.fhem.service.graph.description.SeriesType.WEIGHT;
import static li.klass.fhem.util.ValueExtractUtil.extractLeadingDouble;
import static li.klass.fhem.util.ValueExtractUtil.extractLeadingInt;

@DetailViewSettings(showState = false)
public class WithingsDevice extends Device<WithingsDevice> {
    public enum SubType {
        USER, DEVICE
    }

    @ShowField(description = ResourceIdMapper.fatFreeMass)
    private String fatFreeMass;
    @ShowField(description = ResourceIdMapper.fatMass)
    private String fatMassWeight;
    @ShowField(description = ResourceIdMapper.fatRatio, showInOverview = true)
    private String fatRatio;
    @ShowField(description = ResourceIdMapper.heartPulse)
    private String heartPulse;
    @ShowField(description = ResourceIdMapper.height)
    private String height;
    @ShowField(description = ResourceIdMapper.weight, showInOverview = true)
    private String weight;

    @ShowField(description = ResourceIdMapper.temperature, showInOverview = true)
    private String temperature;
    @ShowField(description = ResourceIdMapper.co2, showInOverview = true)
    private String co2;
    @ShowField(description = ResourceIdMapper.battery)
    private String batteryLevel;

    private SubType subType;

    @Override
    public DeviceFunctionality getDeviceGroup() {
        return DeviceFunctionality.UNKNOWN;
    }

    public String getFatFreeMass() {
        return fatFreeMass;
    }

    public String getFatMassWeight() {
        return fatMassWeight;
    }

    public String getFatRatio() {
        return fatRatio;
    }

    public String getHeartPulse() {
        return heartPulse;
    }

    public String getHeight() {
        return height;
    }

    public String getWeight() {
        return weight;
    }

    public String getTemperature() {
        return temperature;
    }

    public String getCo2() {
        return co2;
    }

    public String getBatteryLevel() {
        return batteryLevel;
    }

    public SubType getSubType() {
        return subType;
    }

    @Override
    public void onChildItemRead(String tagName, String key, String value, NamedNodeMap attributes) {
        super.onChildItemRead(tagName, key, value, attributes);

        if (tagName.equalsIgnoreCase("STATE") &&
                (key.equalsIgnoreCase("BATTERY") ||
                        key.equalsIgnoreCase("WEIGHT"))) {
            setMeasured(attributes.getNamedItem("measured").getNodeValue());
        }
    }

    public void readSUBTYPE(String value) {
        try {
            subType = SubType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            Log.d(WithingsDevice.class.getName(), "cannot find enum value for " + value);
        }
    }

    public void readFATFREEMASS(String value) {
        this.fatFreeMass = formatWeight(value);
    }

    public void readFATMASSWEIGHT(String value) {
        this.fatMassWeight = formatWeight(value);
    }

    public void readFATRATIO(String value) {
        this.fatRatio = "" + extractLeadingDouble(value, 1);
    }

    public void readHEARTPULSE(String value) {
        this.heartPulse = "" + extractLeadingInt(value);
    }

    public void readHEIGHT(String value) {
        this.height = ValueDescriptionUtil.append(value, "m");
    }

    public void readWEIGHT(String value) {
        this.weight = formatWeight(value);
    }

    public void readBATTERYLEVEL(String value) {
        this.batteryLevel = ValueDescriptionUtil.appendPercent(value);
    }

    public void readCO2(String value) {
        this.co2 = ValueDescriptionUtil.append(value, "ppm");
    }

    public void readTEMPERATURE(String value) {
        this.temperature = ValueDescriptionUtil.appendTemperature(value);
    }

    private String formatWeight(String value) {
        return ValueDescriptionUtil.append(extractLeadingDouble(value, 1), "kg");
    }

    @Override
    protected void fillDeviceCharts(List<DeviceChart> chartSeries) {
        super.fillDeviceCharts(chartSeries);

        addDeviceChartIfNotNull(new DeviceChart(R.string.temperatureGraph,
                getRegressionValuesInstance(R.string.temperature, "4:temperature:0",
                        "temperature", TEMPERATURE)), temperature);

        addDeviceChartIfNotNull(new DeviceChart(R.string.co2Graph,
                getRegressionValuesInstance(R.string.co2, "4:co2:0",
                        "temperature", CO2)), co2);

        addDeviceChartIfNotNull(new DeviceChart(R.string.weightGraph),
                getRegressionValuesInstance(R.string.weight, "4:weight:0",
                        "weight", WEIGHT),
                getRegressionValuesInstance(R.string.fatRatio, "4:fatRatio:0",
                        "fatRatio", FAT_RATIO)
        );
    }

    @Override
    public boolean isSupported() {
        return subType != null;
    }
}