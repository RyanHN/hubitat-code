/* Iris v1 Keyboard Driver

Hubitat driver 

FCC ID:FU5TSA04 https://fccid.io/FU5TSA04
Everspring Industry Co Ltd Smart Keypad TSA04



So far this can only be used as a button device. The keyboard will
not send several keys in a lot like a password. You must wait between keypresses.

Its my understanding now that the password would have to be sent to the device and
stored in it. Thats not something I can do I need help on that.

I am working on finding the chime commands.



To Reset for paring:
Remove batteries (I think this is if it was already powered up.)
Press the On key 8 times(Or Hold down ON for 8 seconds)

Insert two batteries side-by-side at one end or the other
Press the On key 8 times
You should see the keypad light up, and the On button will begin to blink twice periodically.



*	09/27/2021  Beta test version Buttons now reporting
                Voltage working/ On OFF turns off switch



https://github.com/tmastersmart/hubitat-code/blob/main/iris_v1_keypad.groovy
https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/iris_v1_keypad.groovy




 * See opensource IRIS code at  https://github.com/arcus-smart-home I have been unable to find any iris v1 code in it




 * based on code from  
   https://github.com/birdslikewires/hubitat

GNU General Public License v3.0
Permissions of this strong copyleft license are conditioned on making available
complete source code of licensed works and modifications, which include larger
works using a licensed work, under the same license. Copyright and license
notices must be preserved. Contributors provide an express grant of patent rights.

 */


import hubitat.zigbee.clusters.iaszone.ZoneStatus


metadata {

	definition (name: "Iris v1 Keyboard", namespace: "tmastersmart", author: "Tmaster", importUrl: "https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/iris_v1_keypad.groovy") {

		capability "Battery"
		capability "Configuration"
		capability "Initialize"
		capability "PresenceSensor"
		capability "Refresh"
		capability "Sensor"
		capability "SignalStrength"
	    capability "Chime"
        capability "Alarm"
		capability "PushableButton"
		capability "Switch" 


		command "checkPresence"
		command "normalMode"
		command "rangingMode"
		//command "quietMode"

		attribute "batteryState", "string"
		attribute "batteryVoltage", "string"

		fingerprint profileId: "C216", inClusters: "00F0,00C0", outClusters: "", manufacturer: "AlertMe", model: "KeyPad Device", deviceJoinName: "Iris V1 Keypad"

	}

}
// fingerprint model:"KeyPad Device", manufacturer:"AlertMe", profileId:"C216", endpointId:"02", inClusters:"00F0,00C0", outClusters:""


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

	sendEvent(name: "battery",value:0, unit: "%", isStateChange: false)
	sendEvent(name: "batteryVoltage", value: 0, unit: "V", isStateChange: false)
	sendEvent(name: "lqi", value: 0, isStateChange: false)
	sendEvent(name: "operation", value: "unknown", isStateChange: false)
	sendEvent(name: "presence", value: "not present", isStateChange: false)
	sendEvent(name: "numberOfButtons", value: "16", isStateChange: false)
    sendEvent(name: "alarm", value: "off", isStateChange: false)    
    

	state.remove("firmwareVersion")	
	state.remove("uptime")
	state.remove("uptimeReceived")
	state.remove("relayClosed")
	state.remove("rssi")
	state.remove("pushed")


	removeDataValue("pushed")

    
    operation

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
	int checkEveryHours = 6																						// Request a ranging report and refresh every 6 hours or every 1 hour for outlets.						
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
	logging("${device} : Received Unknown: cluster: ${map.cluster}, clusterId: ${map.clusterId}, attrId: ${map.attrId}, command: ${map.command} with value: ${map.value} and ${receivedDataCount}data: ${receivedData}", "warn")
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


def quietMode() {

	// Turns off all reporting except for a ranging message every 2 minutes.

	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 03 01} {0xC216}"])
	state.operatingMode = "quiet"

	// We don't receive any of these in quiet mode, so reset them.
	sendEvent(name: "battery",value:0, unit: "%", isStateChange: false)
	sendEvent(name: "operation", value: "quiet")

	logging("${device} : Mode : Quiet", "info")

	refresh()

}


void refresh() {

	logging("${device} : Refreshing", "info")
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F6 {11 00 FC 01} {0xC216}"])	   // version information request

}


def rangeAndRefresh() {

	// This toggles ranging mode to update the device's LQI value.

	int returnToModeSeconds = 6			// We use 3 seconds for outlets, 6 seconds for battery devices, which respond a little more slowly.

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

	if (state.presenceUpdated > 0 && state.batteryOkay == true) {

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

	} else if (state.presenceUpdated > 0 && state.batteryOkay == false) {

		sendEvent(name: "presence", value: "not present")
		logging("${device} : Presence : Battery too low! Reporting not present as this device will no longer be reliable.", "warn")

	} else {

		logging("${device} : Presence : Not yet received. Your device may at max range if so you may have to use built in driver with no presence. ", "warn")

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
		
		logging("${device} : Parse : Failed to parse received data..${description}", "warn")

	}

}





def off() {

   sendEvent(name: "switch", value: "off")
   logging("${device} : Switch OFF", "info") 

    
    
    
//  off sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00EE {11 00 02 00 01} {0xC216}"])
//	on  sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00EE {11 00 02 01 01} {0xC216}"])
//	def cmds = new ArrayList<String>()
//	cmds.add("he raw ${device.deviceNetworkId} 0 2 0x00F5 {11 00 04 01 01} {0xC216}")
//	cmds.add("he raw ${device.deviceNetworkId} 0 2 0x00EE {11 00 05 01 01} {0xC216}")
//	sendZigbeeCommands(cmds)
    
  
    
}


def on() {

   sendEvent(name: "switch", value: "on")
   logging("${device} : Switch ON", "info")  
}


def processMap(Map map) {

	logging("${device} : processMap() : ${map}", "trace")

	// AlertMe values are always sent in a data element.
	String[] receivedData = map.data
    
    
     logging("${device} : debug  Cluster:${map.clusterId}   State:${map.command}", "trace")
    
	if (map.clusterId == "0006") {
		// Match Descriptor Request I have never seen on this device
		logging("${device} : Sending Match Descriptor Response", "debug")
		sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x8006 {00 00 00 01 02} {0xC216}"])
        
    } else if (map.clusterId == "00EF") {
     // Relay actuation and power state messages. 
    logging("${device} : Mains cluster: ${map.clusterId}, attrId: ${map.attrId}, command: ${map.command} with value: ${map.value} data: ${receivedData}", "trace")

    } else if (map.clusterId == "00C0") {
     // Iris Button report cluster 
       
        if (map.command != "00"){
       //  ignore flood of command 00      
       //  Cluster:00C0 CMD:00 MAP:[20, 00]
       //  C216 00C0 02 02 0040 00 2FAA 00 00 0000 00 01 2000
       logging("${device} : key Cluster:${map.clusterId} CMD:${map.command} MAP:${map.data}", "debug")
       }
     
//   Key cluster: 00C0, attrId: null, command: 0A with value: null data: [21, 00, 42, 01, 31] <31 is key 1 32 is key 2
//   debug  Cluster:00C0   State:0A  
     if (map.command == "0A") {   
        keyRec = receivedData[4]

         logging("${device} :KeypadMatrix  [#${keyRec}]", "trace")
         // Keypad matrix         
         if (keyRec == "31"){buttonNumber= 1}
         if (keyRec == "32"){buttonNumber= 2}
         if (keyRec == "33"){buttonNumber= 3}
         if (keyRec == "34"){buttonNumber= 4}
         if (keyRec == "35"){buttonNumber= 5}
         if (keyRec == "36"){buttonNumber= 6}
         if (keyRec == "37"){buttonNumber= 7}
         if (keyRec == "38"){buttonNumber= 8}
         if (keyRec == "39"){buttonNumber= 9}
         if (keyRec == "30"){buttonNumber= 10}

         if (keyRec == "2A"){buttonNumber= 11} // * 
         if (keyRec == "23"){buttonNumber= 12} // #

         if (keyRec == "48"){
             buttonNumber= 13
             sendEvent(name: "switch", value: "off")
             logging("${device} : Switch OFF", "info") 
         } 
         
         if (keyRec == "41"){
             buttonNumber= 14
             sendEvent(name: "switch", value: "on")
             logging("${device} : Switch ON", "info") 
         } 
         
         if (keyRec == "4E"){buttonNumber= 15} // Partial       
         if (keyRec == "50"){buttonNumber= 16} // Panic

                     
         logging("${device} : Button ${buttonNumber} Pressed", "info")
		 sendEvent(name: "pushed", value: buttonNumber, isStateChange: true)   
            
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
		logging("${device} : batteryVoltage sensor value : ${batteryVoltage}", "debug")

		batteryVoltage = batteryVoltage.setScale(3, BigDecimal.ROUND_HALF_UP)

		logging("${device} : batteryVoltage : ${batteryVoltage}", "debug")
		sendEvent(name: "batteryVoltage", value: batteryVoltage, unit: "V")

       // battery doesnt report all batteries only center 2  I get 2.46 volts on 2 good batteries
       // more work needed  
		BigDecimal batteryPercentage = 0
		BigDecimal batteryVoltageScaleMin = 1.00
		BigDecimal batteryVoltageScaleMax = 3.00

		if (batteryVoltage >= batteryVoltageScaleMin && batteryVoltage <= 4.4) {

			state.batteryOkay = true

			batteryPercentage = ((batteryVoltage - batteryVoltageScaleMin) / (batteryVoltageScaleMax - batteryVoltageScaleMin)) * 100.0
			batteryPercentage = batteryPercentage.setScale(0, BigDecimal.ROUND_HALF_UP)
			batteryPercentage = batteryPercentage > 100 ? 100 : batteryPercentage

			if (batteryPercentage > 50) {
				logging("${device} : Battery : $batteryPercentage% ($batteryVoltage V)", "info")
			} else if (batteryPercentage > 30) {
				logging("${device} : Battery : $batteryPercentage% ($batteryVoltage V)", "info")
			} else {
				logging("${device} : Battery : $batteryPercentage% ($batteryVoltage V)", "warn")
			}

			sendEvent(name: "battery", value:batteryPercentage, unit: "%")
            sendEvent(name: "batteryState", value: "ok")

		} else if (batteryVoltage < batteryVoltageScaleMin) {

			// Very low voltages indicate an exhausted battery which requires replacement.

			state.batteryOkay = false

			batteryPercentage = 0

			logging("${device} : Battery : Exhausted battery requires replacement.", "warn")
			logging("${device} : Battery : $batteryPercentage% ($batteryVoltage V)", "warn")
			sendEvent(name: "battery", value:batteryPercentage, unit: "%")

			sendEvent(name: "batteryState", value: "exhausted")

		} else {

			// If the charge circuitry is reporting greater than 4.5 V then the battery is either missing or faulty.
			// THIS NEEDS TESTING ON THE EARLY POWER CLAMP

			state.batteryOkay = false

			batteryPercentage = 0

			logging("${device} : Battery : Exhausted battery requires replacement.", "warn")
			logging("${device} : Battery : $batteryPercentage% ($batteryVoltage V)", "warn")
			sendEvent(name: "battery", value:batteryPercentage, unit: "%")

			sendEvent(name: "batteryState", value: "fault")

		}

		// Report the temperature in celsius.
//		def temperatureValue = "undefined"
//		temperatureValue = receivedData[7..8].reverse().join()
//		logging("${device} : temperatureValue byte flipped : ${temperatureValue}", "trace")
//		BigDecimal temperatureCelsius = hexToBigDecimal(temperatureValue) / 16
//        BigDecimal temperatureF = hexToBigDecimal(temperatureValue)

// No temp sensor
//		logging("${device} : temperatureCelsius sensor value : ${temperatureCelsius}", "trace")
//		logging("${device} : Temperature : $temperatureF", "info")
//		sendEvent(name: "temperature", value: temperatureF, unit: "F")
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

            
            
			logging("${device} : Device : ${deviceModel}", "info")// KeyPad Device
			logging("${device} : Firmware : ${deviceFirmware}", "info")//2013-06-28

			updateDataValue("manufacturer", deviceManufacturer)
            updateDataValue("device", deviceModel)
			updateDataValue("model", "KPD800")
			updateDataValue("firmware", deviceFirmware)
            updateDataValue("fcc", "FU5TSA04")
            updateDataValue("partno", "TSA04-0")
            
		} else {

			// Not a clue what we've received.
			reportToDev(map)

		}


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


private String[] millisToDhms(int millisToParse) {

	long secondsToParse = millisToParse / 1000

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
