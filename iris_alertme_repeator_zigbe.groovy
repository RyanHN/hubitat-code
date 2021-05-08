/*Iris v1 AlertMe Repeator Zigbe



    05/08/2021 v1.2
    04/04/2021 v1.1  
    04/11/2021 v1.0
 
https://github.com/tmastersmart/hubitat-code/raw/main/iris_alertme_repeator_zigbe.groovy



 Bat reports are very often not sure if it reports more with a bad bat

 Repeator plugs have a zigbee and a zwave repeator. You must add them both so 2 drivers




Pressing button on ac gives
       2021-05-04 11:33:24.389 am warnIris AlertMe Repeator Zigbe : Received : cluster: null, clusterId: 00C0, attrId: null, command: 0A with value: null and 4 bits of data: [21, 00, 30, 00]
dev:6142021-05-04 11:33:23.392 am warnIris AlertMe Repeator Zigbe : Received : cluster: null, clusterId: 00C0, attrId: null, command: 0A with value: null and 7 bits of data: [20, 00, 0B, 43, 46, 1A, EA]
dev:6142021-05-04 11:33:19.106 am warnIris AlertMe Repeator Zigbe : Received : cluster: null, clusterId: 00F3, attrId: null, command: 00 with value: null and 6 bits of data: [00, 02, 7F, 73, 00, 00]
dev:6142021-05-04 11:33:18.862 am warnIris AlertMe Repeator Zigbe : Received : cluster: null, clusterId: 00C0, attrId: null, command: 0A with value: null and 4 bits of data: [23, 00, 30, 02]
dev:6142021-05-04 11:33:18.837 am warnIris AlertMe Repeator Zigbe : Received : cluster: null, clusterId: 00F3, attrId: null, command: 01 with value: null and 6 bits of data: [00, 01, 4A, 72, 00, 00]


pressing button on bat gives
dev:6142021-05-04 11:32:33.345 am warnIris AlertMe Repeator Zigbe : Received : cluster: null, clusterId: 00C0, attrId: null, command: 0A with value: null and 4 bits of data: [21, 00, 30, 00]
dev:6142021-05-04 11:32:32.344 am warnIris AlertMe Repeator Zigbe : Received : cluster: null, clusterId: 00C0, attrId: null, command: 0A with value: null and 7 bits of data: [20, 00, 0B, 43, 46, 1A, EA]
dev:6142021-05-04 11:32:27.858 am warnIris AlertMe Repeator Zigbe : Received : cluster: null, clusterId: 00F3, attrId: null, command: 00 with value: null and 6 bits of data: [00, 02, ED, A6, 00, 00]
dev:6142021-05-04 11:32:27.737 am warnIris AlertMe Repeator Zigbe : Received : cluster: null, clusterId: 00C0, attrId: null, command: 0A with value: null and 4 bits of data: [23, 00, 30, 02]
dev:6142021-05-04 11:32:27.716 am warnIris AlertMe Repeator Zigbe : Received : cluster: null, clusterId: 00F3, attrId: null, command: 01 with value: null and 6 bits of data: [00, 01, 37, A6, 00, 00]
















 * using modified UK plug code
 * Forked from https://raw.githubusercontent.com/birdslikewires/hubitat/master/alertme/drivers/alertme_smartplug.groovy
 * name: "AlertMe Smart Plug" 
 * namespace: "BirdsLikeWires", 
 * author: "Andrew Davison", 
 */


metadata {

	definition (name: "Iris AlertMe Repeator Zigbe", namespace: "tmastersmart", author: "tmaster", importUrl: "https://github.com/tmastersmart/hubitat-code/raw/main/iris_alertme_repeator_zigbe.groovy") {

		capability "Battery"
		capability "Configuration"

		capability "Initialize"

		capability "PowerSource"
		capability "PresenceSensor"
		capability "Refresh"
		capability "SignalStrength"

		command "normalMode"
		command "rangingMode"
		command "quietMode"

		attribute "batteryState", "string"
		attribute "batteryVoltage", "string"
		attribute "batteryVoltageWithUnit", "string"
//		attribute "batteryWithUnit", "string"
		attribute "mode", "string"
		attribute "stateMismatch", "boolean"
//		attribute "temperatureWithUnit", "string"
		attribute "uptime", "string"
		attribute "uptimeReadable", "string"

		fingerprint profileId: "C216", inClusters: " 00F0,00F3", outClusters: "", manufacturer: "AlertMe", model: "RepeaterPlug", deviceJoinName: "Iris AlertMe Repeater Plug"
		
	}

}

//manufacturer: AlertMe
//model: RepeaterPlug
//profileId: C216
//inClusters: 00F0,00F3
//firmware: 2013-09-26

preferences {
	
	input name: "infoLogging", type: "bool", title: "Enable logging", defaultValue: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", defaultValue: false
	input name: "traceLogging", type: "bool", title: "Enable trace logging", defaultValue: false
	
}


def installed() {
	// Runs after first pairing.
	logging("${device} : Paired!", "info")
}


def initialize() {

	// Set states to starting values and schedule a single refresh.
	// Runs on reboot, or can be triggered manually.

	// Reset states...

	state.batteryOkay = true
	state.operatingMode = "normal"
	state.presenceUpdated = 0
	state.rangingPulses = 0

	// ...but don't arbitrarily reset the state of the device's main functions or tamper status.

	sendEvent(name: "battery", value:0, unit: "%", isStateChange: false)
	sendEvent(name: "batteryState", value: "unknown", isStateChange: false)
	sendEvent(name: "batteryVoltage", value: 0, unit: "V", isStateChange: false)
	sendEvent(name: "batteryVoltageWithUnit", value: "unknown", isStateChange: false)
//	sendEvent(name: "batteryWithUnit", value: "unknown", isStateChange: false)

	sendEvent(name: "lqi", value: 0)
	sendEvent(name: "operation", value: "unknown", isStateChange: false)

	sendEvent(name: "powerSource", value: "unknown", isStateChange: false)

	sendEvent(name: "presence", value: "not present")
	sendEvent(name: "stateMismatch", value: true, isStateChange: false)

//	sendEvent(name: "temperature", value: 0, unit: "C", isStateChange: false)
//	sendEvent(name: "temperatureWithUnit", value: "unknown", isStateChange: false)
	sendEvent(name: "uptime", value: 0, unit: "s", isStateChange: false)
	sendEvent(name: "uptimeReadable", value: "unknown", isStateChange: false)

	// Remove old settings. Upgrade for debugging
	state.remove("switch")
    state.remove("tamper")
	state.remove("temperature")	
	state.remove("temperatureWithUnit")
	state.remove("uptimeReceived")
	state.remove("presentAt")
	state.remove("relayClosed")
	state.remove("rssi")
	state.remove("supplyPresent")
    state.remove("batteryWithUnit")
   

	// Remove unnecessary device details.
	removeDataValue("application")

	// Stagger our device init refreshes or we run the risk of DDoS attacking our hub on reboot!
	randomSixty = Math.abs(new Random().nextInt() % 60)
	runIn(randomSixty,refresh)

	// Initialisation complete.
	logging("${device} : Initialised", "info")

}


def configure() {

	// Set preferences and ongoing scheduled tasks.
	// Runs after installed() when a device is paired or rejoined, or can be triggered manually.

	initialize()
	unschedule()

	// Default logging preferences.
	device.updateSetting("infoLogging",[value:"true",type:"bool"])
	device.updateSetting("debugLogging",[value:"false",type:"bool"])
	device.updateSetting("traceLogging",[value:"false",type:"bool"])

	// Schedule our ranging report.
	int checkEveryHours = 1																						// Request a ranging report and refresh every 6 hours or every 1 hour for outlets.						
	randomSixty = Math.abs(new Random().nextInt() % 60)
	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
	schedule("${randomSixty} ${randomSixty} ${randomTwentyFour}/${checkEveryHours} * * ? *", rangeAndRefresh)	// At X seconds past X minute, every checkEveryHours hours, starting at Y hour.

	// Schedule the presence check.
	int checkEveryMinutes = 6																					// Check presence timestamp every 6 minutes or every 1 minute for key fobs.						
	randomSixty = Math.abs(new Random().nextInt() % 60)
	schedule("${randomSixty} 0/${checkEveryMinutes} * * * ? *", checkPresence)									// At X seconds past the minute, every checkEveryMinutes minutes.

	// Configuration complete.
	logging("${device} : Configured", "info")

	// Run a ranging report and then switch to normal operating mode.
	rangingMode()
	runIn(12,normalMode)
	
}


def updated() {

	// Runs whenever preferences are saved.

	loggingStatus()
	runIn(3600,debugLogOff)
	runIn(1800,traceLogOff)
	refresh()

}


void loggingStatus() {

	log.info "${device} : Logging : ${infoLogging == true}"
	log.debug "${device} : Debug Logging : ${debugLogging == true}"
	log.trace "${device} : Trace Logging : ${traceLogging == true}"

}


void traceLogOff(){
	
	device.updateSetting("traceLogging",[value:"false",type:"bool"])
	log.trace "${device} : Trace Logging : Automatically Disabled"

}


void debugLogOff(){
	
	device.updateSetting("debugLogging",[value:"false",type:"bool"])
	log.debug "${device} : Debug Logging : Automatically Disabled"

}


void reportToDev(map) {

	String[] receivedData = map.data

	def receivedDataCount = ""
	if (receivedData != null) {
		receivedDataCount = "${receivedData.length} bits of "
	}

//	logging("${device} : UNKNOWN DATA!", "warn")
//	logging("${device} : Received Unknown: cluster: ${map.cluster}, clusterId: ${map.clusterId}, attrId: ${map.attrId}, command: ${map.command} with value: ${map.value} and ${receivedDataCount}data: ${receivedData}", "warn")
//	logging("${device} : Splurge! : ${map}", "trace")

}


def normalMode() {

	// This is the standard running mode.

	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 00 01} {0xC216}"])
	state.operatingMode = "normal"
	refresh()
	sendEvent(name: "operation", value: "normal")
	logging("${device} : Mode : Normal", "info")

}


def rangingMode() {

	// Ranging mode double-flashes (good signal) or triple-flashes (poor signal) the indicator
	// while reporting LQI values. It's also a handy means of identifying or pinging a device.

	// Don't set state.operatingMode here! Ranging is a temporary state only.

	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 01 01} {0xC216}"])
	sendEvent(name: "operation", value: "ranging")
	logging("${device} : Mode : Ranging", "info")

	// Ranging will be disabled after a maximum of 30 pulses.
	state.rangingPulses = 0

}


def lockedMode() {

	// Locked mode is not as useful as it might first appear. This disables the local power button on
	// the outlet. However, this can be reset by rebooting the outlet by holding that same power
	// button for ten seconds. Or you could just turn off the supply, of course.

	// To complicate matters this mode cannot be disabled remotely, so far as I can tell.

	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 02 01} {0xC216}"])
	refresh()
	state.operatingMode = "locked"
	sendEvent(name: "operation", value: "locked")

	logging("${device} : Mode : Locked", "info")

}


def quietMode() {

	// Turns off all reporting except for a ranging message every 2 minutes.

	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 03 01} {0xC216}"])
	state.operatingMode = "quiet"

	// We don't receive any of these in quiet mode, so reset them.
	sendEvent(name: "battery", value:0, unit: "%", isStateChange: false)
	sendEvent(name: "batteryVoltage", value: 0, unit: "V", isStateChange: false)
	sendEvent(name: "batteryVoltageWithUnit", value: ".", isStateChange: false)
//	sendEvent(name: "batteryWithUnit", value: ".", isStateChange: false)
//	sendEvent(name: "energy", value: 0, unit: "kWh", isStateChange: false)
//	sendEvent(name: "energyWithUnit", value: "unknown", isStateChange: false)
	sendEvent(name: "operation", value: "quiet")
//	sendEvent(name: "power", value: 0, unit: "W", isStateChange: false)
//	sendEvent(name: "powerWithUnit", value: "unknown", isStateChange: false)
	sendEvent(name: "uptime", value: 0, unit: "s", isStateChange: false)
	sendEvent(name: "uptimeReadable", value: "unknown", isStateChange: false)
//	sendEvent(name: "temperature", value: 0, unit: "C", isStateChange: false)
//	sendEvent(name: "temperatureWithUnit", value: "unknown", isStateChange: false)

	logging("${device} : Mode : Quiet", "info")

	refresh()

}





void refresh() {

	// The Smart Plug becomes remote controllable after joining once it has received confirmation of the power control operating mode.
	// It also expects the Hub to check in with this occasionally, otherwise remote control is eventually dropped.
	// Whenever a refresh happens (which is at least hourly using rangeAndRefresh() for outlets) we send the nudge.

	logging("${device} : Refreshing", "info")

	def cmds = new ArrayList<String>()
	cmds.add("he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F6 {11 00 FC 01} {0xC216}")    // version information request
	cmds.add("he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00EE {11 00 01 01} {0xC216}")    // power control operating mode nudge
	sendZigbeeCommands(cmds)

}


def rangeAndRefresh() {

	// This toggles ranging mode to update the device's LQI value.

	int returnToModeSeconds = 3			// We use 3 seconds for outlets, 6 seconds for battery devices, which respond a little more slowly.

	rangingMode()
	runIn(returnToModeSeconds, "${state.operatingMode}Mode")

}


def updatePresence() {

	long millisNow = new Date().time
	state.presenceUpdated = millisNow

}


def checkPresence() {

	// Check how long ago the presence state was updated.

	// AlertMe devices check in with some sort of report at least every 2 minutes (every minute for outlets).

	// It would be suspicious if nothing was received after 4 minutes, but this check runs every 6 minutes
	// by default (every minute for key fobs) so we don't exaggerate a wayward transmission or two.

	presenceTimeoutMinutes = 4
	uptimeAllowanceMinutes = 5

	if (state.presenceUpdated > 0) {

		long millisNow = new Date().time
		long millisElapsed = millisNow - state.presenceUpdated
		long presenceTimeoutMillis = presenceTimeoutMinutes * 60000
		BigInteger secondsElapsed = BigDecimal.valueOf(millisElapsed / 1000)
		BigInteger hubUptime = location.hub.uptime

		if (millisElapsed > presenceTimeoutMillis) {

			if (hubUptime > uptimeAllowanceMinutes * 60) {

				sendEvent(name: "presence", value: "not present")
				logging("${device} : Presence : Not Present! Last report received ${secondsElapsed} seconds ago.", "warn")

			} else {

				logging("${device} : Presence : Ignoring overdue presence reports for ${uptimeAllowanceMinutes} minutes. The hub was rebooted ${hubUptime} seconds ago.", "info")

			}

		} else {

			sendEvent(name: "presence", value: "present")
			logging("${device} : Presence : Last presence report ${secondsElapsed} seconds ago.", "debug")

		}

		logging("${device} : checkPresence() : ${millisNow} - ${state.presenceUpdated} = ${millisElapsed} (Threshold: ${presenceTimeoutMillis} ms)", "trace")

	} else {

		logging("${device} : Presence : Waiting for first presence report.", "warn")

	}

}


def parse(String description) {

	// Primary parse routine.

	logging("${device} : Parse : $description", "debug")

	sendEvent(name: "presence", value: "present")
	updatePresence()

	Map descriptionMap = zigbee.parseDescriptionAsMap(description)

	if (descriptionMap) {

		processMap(descriptionMap)

	} else {
		
		logging("${device} : Parse : Failed to parse received data. Please report these messages to the developer.", "warn")
		logging("${device} : Splurge! : ${description}", "warn")

	}

}


def processMap(Map map) {

	logging("${device} : processMap() : ${map}", "trace")

	// AlertMe values are always sent in a data element.
	String[] receivedData = map.data

//	if (map.clusterId == "00EE") { wrong cluster for usa version must be 00F3
    if (map.clusterId == "00F3") {   

		// Relay actuation and power state messages.

//		if (map.command == "80") { does not work
        if (map.command == "00") {    

			// Power States

			def powerStateHex = "undefined"
			powerStateHex = receivedData[2]
            
            logging("${device} : debug  Cluster:${map.clusterId}   State:${powerStateHex}  MAP:${map.data}", "warn")

//      Received : cluster: null, clusterId: 00F3, attrId: null, command: 00 with value: null and 6 bits of data: [00, 02, ED, A6, 00, 00]
//      Received : cluster: null, clusterId: 00F3, attrId: null, command: 01 with value: null and 6 bits of data: [00, 01, 37, A6, 00, 00]

            
// Received      : cluster: null, clusterId: 00F3, attrId: null, command: 00 with value: null and 6 bits of data: [00, 02, 2F, 7A, 00, 00]            
//Received Unknown: cluster: null, clusterId: 00F3, attrId: null, command: 01 with value: null and 6 bits of data: [00, 01, 0F, 20, 00, 00]           
			// Power states are fun.
			//   00 00 - Cold mains power on with relay off (only occurs when battery dead or after reset)
			//   01 01 - Cold mains power on with relay on (only occurs when battery dead or after reset)
			//   02 00 - Mains power off and relay off [BATTERY OPERATION]
			//   03 01 - Mains power off and relay on [BATTERY OPERATION]
			//   04 00 - Mains power returns with relay off (only follows a 00 00)
			//   05 01 - Mains power returns with relay on (only follows a 01 01)
			//   06 00 - Mains power on and relay off (normal actuation)
			//   07 01 - Mains power on and relay on (normal actuation)

			if (powerStateHex == "02" || powerStateHex == "03") {

				// Supply failed.

				sendEvent(name: "batteryState", value: "discharging")
				sendEvent(name: "powerSource", value: "battery")
//				sendEvent(name: "tamper", value: "detected")
				state.supplyPresent = false

				// Whether this is a problem!

				if (powerStateHex == "02") {

					logging("${device} : Supply : Incoming supply failure with relay open.", "warn")
					sendEvent(name: "stateMismatch", value: false, isStateChange: true)

				} else {

					logging("${device} : Supply : Incoming supply failure with relay closed. CANNOT POWER LOAD!", "warn")
					sendEvent(name: "stateMismatch", value: true, isStateChange: true)

				}

			} else if (powerStateHex == "54" || powerStateHex == "9C") {

				// Supply present.

				state.supplyPresent ?: logging("${device} : Supply : Incoming supply has returned.", "info")
				state.supplyPresent ?: sendEvent(name: "batteryState", value: "charging")

				sendEvent(name: "stateMismatch", value: false)
				sendEvent(name: "powerSource", value: "mains")
//				sendEvent(name: "tamper", value: "clear")
				state.supplyPresent = true

			} else {

				// Supply returned!

				logging("${device} : Supply : Device returning from shutdown, please check batteries! hex code ${powerStateHex}  ", "warn")

				if (state.batteryOkay) {
					sendEvent(name: "batteryState", value: "charging")
				}

				sendEvent(name: "stateMismatch", value: false)
				sendEvent(name: "powerSource", value: "mains")
//				sendEvent(name: "tamper", value: "clear")
				state.supplyPresent = true

			}

			// Relay States    not used on repeator

//			def switchStateHex = "undefined"
//			switchStateHex = receivedData[1]
//
//			if (switchStateHex == "01") {
//
//				state.relayClosed = true
//				sendEvent(name: "switch", value: "on")
//				logging("${device} : Switch : On", "info")
//
//			} else {
//
//				state.relayClosed = false
//				sendEvent(name: "switch", value: "off")
//				logging("${device} : Switch : Off", "info")
//
//			}

		} else {

			reportToDev(map)

		}

	} else if (map.clusterId == "00EF") {

		// Power and energy messages.

		if (map.command == "81") {

			// Power Reading

			def powerValueHex = "undefined"
			int powerValue = 0

			// These power readings are so frequent that we only log them in debug or trace.
			powerValueHex = receivedData[0..1].reverse().join()
			logging("${device} : power byte flipped : ${powerValueHex}", "trace")
			powerValue = zigbee.convertHexToInt(powerValueHex)
			logging("${device} : power sensor reports : ${powerValue}", "debug")

//			sendEvent(name: "power", value: powerValue, unit: "W")
//			sendEvent(name: "powerWithUnit", value: "${powerValue} W")

		} else if (map.command == "82") {

			// Command 82 returns energy summary in watt-hours with an uptime counter.

	

			// Uptime

			String uptimeValueHex = "undefined"
			uptimeValueHex = receivedData[4..8].reverse().join()
			logging("${device} : uptime byte flipped : ${uptimeValueHex}", "trace")

			BigInteger uptimeValue = new BigInteger(uptimeValueHex, 16)
			logging("${device} : uptime counter reports : ${uptimeValue}", "debug")

			def newDhmsUptime = []
			newDhmsUptime = millisToDhms(uptimeValue * 1000)
			String uptimeReadable = "${newDhmsUptime[3]}d ${newDhmsUptime[2]}h ${newDhmsUptime[1]}m"

			logging("${device} : Uptime : ${uptimeReadable}", "debug")

			sendEvent(name: "uptime", value: uptimeValue, unit: "s")
			sendEvent(name: "uptimeReadable", value: uptimeReadable)

		} else {

			// Unknown power or energy data.
			reportToDev(map)

		}

	} else if (map.clusterId == "00F0") {

		// Device status cluster.

		// Report the battery voltage and calculated percentage.
		def batteryVoltageHex = "undefined"
		BigDecimal batteryVoltage = 0

		batteryVoltageHex = receivedData[5..6].reverse().join()
		logging("${device} : batteryVoltageHex byte flipped : ${batteryVoltageHex}", "trace")

		if (batteryVoltageHex == "FFFF") {
			// Occasionally a weird battery reading can be received. Ignore it.
			logging("${device} : batteryVoltageHex skipping anomolous reading : ${batteryVoltageHex}", "debug")
			return
		}

		batteryVoltage = zigbee.convertHexToInt(batteryVoltageHex) / 1000
		//logging("${device} : batteryVoltage sensor value : ${batteryVoltage}", "debug")

		if (getDataValue("firmware").startsWith("2010")) {
			// Early firmware fudges the voltage reading to match other 3 volt battery devices. Cheeky.
			// This converts to a reasonable approximation of the actual voltage. All newer firmwares report accurately.
			batteryVoltage = batteryVoltage * 1.40
			logging("${device} : early firmware requires batteryVoltage correction!", "debug")
		}

		batteryVoltage = batteryVoltage.setScale(3, BigDecimal.ROUND_HALF_UP)

		logging("${device} : batteryVoltage : ${batteryVoltage}", "debug")
		sendEvent(name: "batteryVoltage", value: batteryVoltage, unit: "V")
		sendEvent(name: "batteryVoltageWithUnit", value: "${batteryVoltage} V")

		BigDecimal batteryPercentage = 0
		BigDecimal batteryVoltageScaleMin = 3.10 // 3v would be 1 volt per cell
		BigDecimal batteryVoltageScaleMax = 4.15

		if (batteryVoltage >= batteryVoltageScaleMin && batteryVoltage <= 4.40) {

			// A good three-cell 3.6 V NiMH battery will sit between 4.10 V and 4.25 V. 

			state.batteryOkay = true

			batteryPercentage = ((batteryVoltage - batteryVoltageScaleMin) / (batteryVoltageScaleMax - batteryVoltageScaleMin)) * 100.0
			batteryPercentage = batteryPercentage.setScale(0, BigDecimal.ROUND_HALF_UP)
			batteryPercentage = batteryPercentage > 100 ? 100 : batteryPercentage 
            
			if (batteryPercentage > 10) {
				logging("${device} : Battery : $batteryPercentage% ($batteryVoltage V)", "info")
			} else {
				logging("${device} : Battery : $batteryPercentage% ($batteryVoltage V)", "warn")
			}

			sendEvent(name: "battery", value:batteryPercentage, unit: "%")
//			sendEvent(name: "batteryWithUnit", value:"${batteryPercentage} %")

			if (batteryVoltage > batteryVoltageScaleMax) {
				!state.supplyPresent ?: sendEvent(name: "batteryState", value: "charged")
			} else {
				!state.supplyPresent ?: sendEvent(name: "batteryState", value: "charging")
			}

		} else if (batteryVoltage < batteryVoltageScaleMin) {

			// Very low voltages indicate an exhausted battery which requires replacement.

			state.batteryOkay = false

			batteryPercentage = 0

//			logging("${device} : Battery : Exhausted battery.", "warn")   To many warnings
			logging("${device} : Battery : $batteryPercentage% ($batteryVoltage V)", "warn")
			sendEvent(name: "battery", value:batteryPercentage, unit: "%")
//			sendEvent(name: "batteryWithUnit", value:"${batteryPercentage} %")
			sendEvent(name: "batteryState", value: "exhausted")

		} else {

			// If the charge circuitry is reporting greater than 4.5 V then the battery is either missing or faulty.

			state.batteryOkay = false

			batteryPercentage = 0

			logging("${device} : Battery : Exhausted battery requires replacement.", "warn")
			logging("${device} : Battery : $batteryPercentage% ($batteryVoltage V)", "warn")
			sendEvent(name: "battery", value:batteryPercentage, unit: "%")
//			sendEvent(name: "batteryWithUnit", value:"${batteryPercentage} %")
			sendEvent(name: "batteryState", value: "fault")

		}

		// Report the temperature in celsius.
		def temperatureValue = "undefined"
		temperatureValue = receivedData[7..8].reverse().join()
//		logging("${device} : temperatureValue byte flipped : ${temperatureValue}", "trace")
//		BigDecimal temperatureCelsius = hexToBigDecimal(temperatureValue) / 16
//		BigDecimal temperatureF = hexToBigDecimal(temperatureValue) 
//		logging("${device} : temperatureCelsius sensor value : ${temperatureCelsius}", "trace")
//		logging("${device} : temperatureF sensor value : ${temperatureF}", "trace")

		// I've commented out the correction code below as there's really no point in trying to fudge these values to get a room temperature.
		// Other devices do room temperature properly and these values may be useful as plug overheat warnings, so let's not mess.

		// // Smart plugs warm up while being used, so here's how we attempt to correct for this.
		// BigDecimal correctionValue = (state.relayClosed) ? 0.6 : 0.75
		// BigDecimal temperatureCelsiusCorrected = Math.round(temperatureCelsius * correctionValue * 100) / 100
		// logging("${device} : temperatureCelsiusCorrected : ${temperatureCelsiusCorrected} = ${temperatureCelsius} x ${correctionValue}", "trace")
		// logging("${device} : Corrected Temperature : ${temperatureCelsiusCorrected} C", "debug")

///		sendEvent(name: "temperature", value: temperatureF, unit: "F")
//		sendEvent(name: "temperatureWithUnit", value: "${temperatureF} °F")

	} else if (map.clusterId == "00F6") {

		// Discovery cluster. 

		if (map.command == "FD") {

			// Ranging is our jam, Hubitat deals with joining on our behalf.

			def lqiRangingHex = "undefined"
			int lqiRanging = 0
			lqiRangingHex = receivedData[0]
			lqiRanging = zigbee.convertHexToInt(lqiRangingHex)
			sendEvent(name: "lqi", value: lqiRanging)
			logging("${device} : lqiRanging : ${lqiRanging}", "debug")

			if (receivedData[1] == "77") {

				// This is ranging mode, which must be temporary. Make sure we come out of it.
				state.rangingPulses++
				if (state.rangingPulses > 30) {
					"${state.operatingMode}Mode"()
				}

			} else if (receivedData[1] == "FF") {

				// This is the ranging report received every 30 seconds while in quiet mode.
				logging("${device} : quiet ranging report received", "debug")

			} else if (receivedData[1] == "00") {

				// This is the ranging report received when the device reboots.
				// After rebooting a refresh is required to bring back remote control.
				logging("${device} : reboot ranging report received", "debug")
				refresh()

			} else {

				// Something to do with ranging we don't know about!
				reportToDev(map)

			} 

		} else if (map.command == "FE") {

			// Device version response.

			def versionInfoHex = receivedData[31..receivedData.size() - 1].join()

			StringBuilder str = new StringBuilder()
			for (int i = 0; i < versionInfoHex.length(); i+=2) {
				str.append((char) Integer.parseInt(versionInfoHex.substring(i, i + 2), 16))
			} 

			String versionInfo = str.toString()
			String[] versionInfoBlocks = versionInfo.split("\\s")
			int versionInfoBlockCount = versionInfoBlocks.size()
			String versionInfoDump = versionInfoBlocks[0..versionInfoBlockCount - 1].toString()

			logging("${device} : device version received in ${versionInfoBlockCount} blocks : ${versionInfoDump}", "debug")

			String deviceManufacturer = "AlertMe"
			String deviceModel = ""
			String deviceFirmware = versionInfoBlocks[versionInfoBlockCount - 1]

			// Sometimes the model name contains spaces.
			if (versionInfoBlockCount == 2) {
				deviceModel = versionInfoBlocks[0]
			} else {
				deviceModel = versionInfoBlocks[0..versionInfoBlockCount - 2].join(' ').toString()
			}

			logging("${device} : Device : ${deviceModel}", "info")
			logging("${device} : Firmware : ${deviceFirmware}", "info")

			updateDataValue("manufacturer", deviceManufacturer)
			updateDataValue("model", deviceModel)
			updateDataValue("firmware", deviceFirmware)

		} else {

			// Not a clue what we've received.
			reportToDev(map)

		}

	} else if (map.clusterId == "8001" || map.clusterId == "8038") {

		// These clusters are sometimes received from the SPG100 and I have no idea why.
		//   8001 arrives with 12 bytes of data
		//   8038 arrives with 27 bytes of data
		logging("${device} : Skipping data received on cluserId ${map.clusterId}.", "debug")

	} else if (map.clusterId == "8032" ) {

		// These clusters are sometimes received when joining new devices to the mesh.
		//   8032 arrives with 80 bytes of data, probably routing and neighbour information.
		// We don't do anything with this, the mesh re-jigs itself and is a known thing with AlertMe devices.
		logging("${device} : New join has triggered a routing table reshuffle.", "debug")

	} else {

		// Not a clue what we've received.
		reportToDev(map)

	}

	return null

}


void sendZigbeeCommands(List<String> cmds) {

	// All hub commands go through here for immediate transmission and to avoid some method() weirdness.

    logging("${device} : sendZigbeeCommands received : ${cmds}", "trace")
    sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))

}


private String[] millisToDhms(BigInteger millisToParse) {

	BigInteger secondsToParse = millisToParse / 1000

	def dhms = []
	dhms.add(secondsToParse % 60)
	secondsToParse = secondsToParse / 60
	dhms.add(secondsToParse % 60)
	secondsToParse = secondsToParse / 60
	dhms.add(secondsToParse % 24)
	secondsToParse = secondsToParse / 24
	dhms.add(secondsToParse % 365)
	return dhms

}


private BigDecimal hexToBigDecimal(String hex) {
    int d = Integer.parseInt(hex, 16) << 21 >> 21
    return BigDecimal.valueOf(d)
}


private boolean logging(String message, String level) {

	boolean didLog = false

	if (level == "error") {
		log.error "$message"
		didLog = true
	}

	if (level == "warn") {
		log.warn "$message"
		didLog = true
	}

	if (traceLogging && level == "trace") {
		log.trace "$message"
		didLog = true
	}

	if (debugLogging && level == "debug") {
		log.debug "$message"
		didLog = true
	}

	if (infoLogging && level == "info") {
		log.info "$message"
		didLog = true
	}

	return didLog

}
