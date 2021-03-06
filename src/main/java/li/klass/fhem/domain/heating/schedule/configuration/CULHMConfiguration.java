/*
 * AndFHEM - Open Source Android application to control a FHEM home automation
 * server.
 *
 * Copyright (c) 2011, Matthias Klass or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU GENERAL PUBLIC LICENSE, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU GENERAL PUBLIC LICENSE
 * for more details.
 *
 * You should have received a copy of the GNU GENERAL PUBLIC LICENSE
 * along with this distribution; if not, write to:
 *   Free Software Foundation, Inc.
 *   51 Franklin Street, Fifth Floor
 *   Boston, MA  02110-1301  USA
 */

package li.klass.fhem.domain.heating.schedule.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

import li.klass.fhem.domain.CULHMDevice;
import li.klass.fhem.domain.heating.schedule.DayProfile;
import li.klass.fhem.domain.heating.schedule.WeekProfile;
import li.klass.fhem.domain.heating.schedule.interval.FilledTemperatureInterval;
import li.klass.fhem.util.DayUtil;

import static com.google.common.collect.Lists.newArrayList;

public class CULHMConfiguration extends HeatingConfiguration<FilledTemperatureInterval, CULHMDevice, CULHMConfiguration> {

    public static final int MAXIMUM_NUMBER_OF_HEATING_INTERVALS = 24;

    public CULHMConfiguration() {
        super("", MAXIMUM_NUMBER_OF_HEATING_INTERVALS, NumberOfIntervalsType.DYNAMIC);
    }

    private static final Logger LOG = LoggerFactory.getLogger(CULHMConfiguration.class);

    @Override
    public void readNode(WeekProfile<FilledTemperatureInterval, CULHMConfiguration, CULHMDevice> weekProfile, String key, String value) {
        if (!key.matches("(R_(P1_)?[0-9]_)?TEMPLIST([A-Z]{3})")) return;

        String shortName = key.replaceAll("(R_(P1_)?[0-9]_)?TEMPLIST", "");
        DayUtil.Day day = DayUtil.getDayForShortName(shortName);
        if (day == null) return;

        String[] parts = value.replaceAll("set[_]?[ ]?", "").trim().split(" ");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            int index = i / 2;

            FilledTemperatureInterval interval = getOrCreateInterval(weekProfile, day, index);
            if (i % 2 == 0) { // time desc
                interval.setSwitchTime(part);
                if (part.equals("24:00")) {
                    interval.setTimeFixed(true);
                }
            } else { //temperature desc
                interval.setTemperature(Double.valueOf(part));
            }
        }
    }

    @Override
    public DayProfile<FilledTemperatureInterval, CULHMDevice, CULHMConfiguration>
    createDayProfileFor(DayUtil.Day day, CULHMConfiguration configuration) {

        return new DayProfile<>(day, configuration);
    }

    @Override
    public List<String> generateScheduleCommands(CULHMDevice device, WeekProfile<FilledTemperatureInterval, CULHMConfiguration, CULHMDevice> weekProfile) {
        List<String> result = newArrayList();
        List<DayProfile<FilledTemperatureInterval, CULHMDevice, CULHMConfiguration>> changedDayProfiles = weekProfile.getChangedDayProfiles();
        LOG.info("generateScheduleCommands - {} day(s) contain changes", changedDayProfiles.size());
        for (DayProfile<FilledTemperatureInterval, CULHMDevice, CULHMConfiguration> dayProfile : changedDayProfiles) {
            result.add(generateCommandFor(device, dayProfile));
        }

        return result;
    }

    public String generateCommandFor(CULHMDevice device, DayProfile<FilledTemperatureInterval, CULHMDevice, CULHMConfiguration> dayProfile) {
        StringBuilder command = new StringBuilder();

        List<FilledTemperatureInterval> heatingIntervals = newArrayList(dayProfile.getHeatingIntervals());
        Collections.sort(heatingIntervals);

        for (int i = 0; i < dayProfile.getNumberOfHeatingIntervals(); i++) {
            FilledTemperatureInterval interval = heatingIntervals.get(i);
            if (i != 0) {
                command.append(" ");
            }
            command.append(interval.getChangedSwitchTime()).append(" ").append(interval.getChangedTemperature());
        }

        String shortName = DayUtil.getShortNameFor(dayProfile.getDay());
        char firstChar = shortName.charAt(0);
        shortName = ((char) (firstChar - 'a' + 'A')) + shortName.substring(1);

        return "set " + device.getName() + " tempList" + shortName + " " + command.toString();
    }

    @Override
    public void afterXMLRead(WeekProfile<FilledTemperatureInterval, CULHMConfiguration, CULHMDevice> weekProfile) {
        super.afterXMLRead(weekProfile);

        List<DayProfile<FilledTemperatureInterval, CULHMDevice, CULHMConfiguration>>
                profiles = weekProfile.getSortedDayProfiles();

        for (DayProfile<FilledTemperatureInterval, CULHMDevice, CULHMConfiguration> profile : profiles) {
            addFixedMidnightIntervalIfRequired(profile);
        }
    }

    /**
     * One interval always has to relate to midnight. For CUL_HM, this is always the last one,
     * representing the time _until_ midnight. The method adds this interval if not being already
     * present.
     *
     * @param dayProfile day profile to add the midnight to.
     */
    private void addFixedMidnightIntervalIfRequired(
            DayProfile<FilledTemperatureInterval, CULHMDevice, CULHMConfiguration> dayProfile) {

        List<FilledTemperatureInterval> intervals = dayProfile.getHeatingIntervals();

        boolean found = false;
        for (FilledTemperatureInterval interval : intervals) {
            if ("24:00".equals(interval.getSwitchTime())) {
                found = true;
                break;
            }
        }

        if (!found) {
            FilledTemperatureInterval heatingInterval = createHeatingInterval();

            heatingInterval.setChangedSwitchTime("24:00");
            heatingInterval.setChangedTemperature(CULHMDevice.MINIMUM_TEMPERATURE);
            heatingInterval.setTimeFixed(true);

            dayProfile.addHeatingInterval(heatingInterval);
        }
    }

    @Override
    public FilledTemperatureInterval createHeatingInterval() {
        return new FilledTemperatureInterval();
    }

    @Override
    public IntervalType getIntervalType() {
        return IntervalType.TO;
    }
}
