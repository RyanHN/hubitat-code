/**
 *  ****************  Hub Watchdog Examiner App  ****************
 *
 *  Design Usage:
 *  Simple way to monitor if your hub is slowing down or not.
 *
 *  Copyright 2019-2020 Bryan Turcotte (@bptworld)
 * 
 *  This App is free.  If you like and use this app, please be sure to mention it on the Hubitat forums!  Thanks.
 *
 *  Remember...I am not a programmer, everything I do takes a lot of time and research!
 *  Donations are never necessary but always appreciated.  Donations to support development efforts are accepted via: 
 *
 *  Paypal at: https://paypal.me/bptworld
 *
 *-------------------------------------------------------------------------------------------------------------------
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 * ------------------------------------------------------------------------------------------------------------------------------
 *
 *  If modifying this project, please keep the above header intact and add your comments/credits below - Thank you! -  @BPTWorld
 *
 *  App and Driver updates can be found at https://github.com/bptworld/Hubitat/
 *
 * ------------------------------------------------------------------------------------------------------------------------------
 *
 *  Changes:
 *
 *  1.0.8 - 09/24/20 - Adjustments
 *  1.0.7 - 09/01/20 - Cosmetic changes, changes to handle 40 points
 *  1.0.6 - 05/01/20 - Fixed the error when no data, thanks axornet
 *  1.0.5 - 04/30/20 - Fixed a nasty bug
 *  1.0.4 - 04/27/20 - Cosmetic changes
 *  1.0.3 - 04/25/20 - Major rewrite by axornet, including addition of Graphs! Thank you.
 *  1.0.2 - 12/04/19 - Chasing a gremlin
 *  1.0.1 - 09/30/19 - Lots of little changes
 *  1.0.0 - 09/29/19 - Initial release.
 *
 */

import hubitat.helper.RMUtils
import groovy.time.TimeCategory
import java.text.SimpleDateFormat

def setVersion(){
    state.name = "Hub Watchdog Examiner"
	state.version = "1.0.8"
}

definition(
    name: "Hub Watchdog Examiner Child",
    namespace: "BPTWorld",
    author: "Bryan Turcotte",
    description: "Simple way to monitor if your hub is slowing down or not.",
    category: "",
	parent: "BPTWorld:Hub Watchdog",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
	importUrl: "",
)

preferences {
    page name: "pageConfig"
    page name: "reportRawOptions", title: "", install: false, uninstall: true, nextPage: "pageConfig"
    page name: "reportSummaryOptions", title: "", install: false, uninstall: true, nextPage: "pageConfig"
}

def pageConfig() {
    dynamicPage(name: "", title: "", install: true, uninstall: true) {
		display() 
        section("${getImage('instructions')} <b>Instructions:</b>", hideable: true, hidden: true) {
            paragraph "Simple way to monitor if your hub is slowing down or not."
			paragraph "<b>Notes:</b>"
			paragraph "- You can use any type of 'switched' device you want to test. Virtual, Zwave or Zigbee<br>- Remember, any device you use will turn off after 5 seconds to test.<br>- Best to use an extra plugin module for testing."
		}  
        section(getFormat("header-green", "${getImage("Blank")}"+" Data Devices")) {
			input(name: "getDevice1", type: "capability.actuator", title: "Device 1 to compare:", submitOnChange: true, width: 4)
            input(name: "getDevice2", type: "capability.actuator", title: "Device 2 to compare:", submitOnChange: true, width: 4)
            input(name: "getDevice3", type: "capability.actuator", title: "Device 3 to compare:", submitOnChange: true, width: 4)
        }
        section(getFormat("header-green", "${getImage("Blank")}"+" Color Coding the Raw Data")) {
            paragraph "Color code data for:"
            input "colorZwav", "text", title: "Zw", submitOnChange: true, width: 3
            input "colorZigb", "text", title: "Zb", submitOnChange: true, width: 3
            input "colorVirt", "text", title: "Vt", submitOnChange: true, width: 3
            input "colorOther", "text", title: "O", submitOnChange: true, width: 3
        }
        section(getFormat("header-green", "${getImage("Blank")}"+" Report Summary Data")) {
            if(getDevice1 == null || getDevice1 == "") state.reportStats1 = "Nothing to report"
            if(getDevice2 == null || getDevice2 == "") state.reportStats2 = "Nothing to report"
            if(getDevice3 == null || getDevice3 == "") state.reportStats3 = "Nothing to report"
            
            paragraph "<table width='100%' border='1'><tr><td width='33%'>${state.reportStats1}</td><td width='33%'>${state.reportStats2}</td><td width='33%'>${state.reportStats3}</td></tr></table>"
        }
        section(getFormat("header-green", "${getImage("Blank")}"+" Reports")) {
            href "reportRawOptions", title: "Raw Data Report", description: "Click here to view the Raw Data Report."
		}
        
        section(getFormat("header-green", "${getImage("Blank")}"+" App Control")) {
            input "pauseApp", "bool", title: "Pause App", defaultValue:false, submitOnChange:true            
            if(pauseApp) {
                if(app.label) {
                    if(!app.label.contains(" (Paused)")) {
                        app.updateLabel(app.label + " (Paused)")
                    }
                }
            } else {
                if(app.label) {
                    app.updateLabel(app.label - " (Paused)")
                }
            }
            paragraph "This app can be enabled/disabled by using a switch. The switch can also be used to enable/disable several apps at the same time."
            input "disableSwitch", "capability.switch", title: "Switch Device(s) to Enable / Disable this app", submitOnChange:true, required:false, multiple:true
        }
        
        section(getFormat("header-green", "${getImage("Blank")}"+" General")) {
            label title: "Enter a name for this automation", required: false
            input "logEnable", "bool", defaultValue:false, title: "Enable Debug Logging", description: "debugging", submitOnChange:true
		}
		display2()
	}
}

def installed() {
    log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {	
    if(logEnable) log.debug "Updated with settings: ${settings}"
    unsubscribe()
	unschedule()
    if(logEnable) runIn(3600, logsOff)
	initialize()
}

def initialize() {
    checkEnableHandler()
    if(pauseApp || state.eSwitch) {
        log.info "${app.label} is Paused or Disabled"
    } else {
        setDefaults()
        if(getDevice1) subscribe(getDevice1, "list1", getRawData)
        if(getDevice2) subscribe(getDevice2, "list1", getRawData)
        if(getDevice3) subscribe(getDevice3, "list1", getRawData)
    }
}

def getRawData(evt){
    checkEnableHandler()
    if(pauseApp || state.eSwitch) {
        log.info "${app.label} is Paused or Disabled"
    } else {
        // *** Raw Data ***
        if(logEnable) log.debug "In getTheData (${state.version})"
        if(getDevice1) deviceData1 = getDevice1.currentValue("list1")
        if(getDevice2) deviceData2 = getDevice2.currentValue("list1")
        if(getDevice3) deviceData3 = getDevice3.currentValue("list1")

        if(deviceData1) deviceD1 = deviceData1.replace("["," ").replace("]","")
        if(deviceData2) deviceD2 = deviceData2.replace("["," ").replace("]","")
        if(deviceData3) deviceD3 = deviceData3.replace("["," ").replace("]","")
        state.deviceData = [deviceD1, deviceD2, deviceD3].flatten().findAll{it}

        // *** Summary Data ***
        if(logEnable) log.debug "In getDataSummary (${state.version})"
        if(getDevice1) {
            meanD1 = getDevice1.currentValue("meanD")
            medianD1 = getDevice1.currentValue("medianD")
            minimumD1 = getDevice1.currentValue("minimumD")
            maximumD1 = getDevice1.currentValue("maximumD")
            readingsSize1 = getDevice1.currentValue("readingsSize1")
            listSizeB1 = getDevice1.currentValue("listSizeB")
            listSizeW1 = getDevice1.currentValue("listSizeW")
            maxDelay1 = getDevice1.currentValue("maxDelay")
            warnValue1 = getDevice1.currentValue("warnValue")
            lastUpdated1 = getDevice1.currentValue("lastUpdated")
        }
        if(getDevice2) {
            meanD2 = getDevice2.currentValue("meanD")
            medianD2 = getDevice2.currentValue("medianD")
            minimumD2 = getDevice2.currentValue("minimumD")
            maximumD2 = getDevice2.currentValue("maximumD")
            readingsSize2 = getDevice2.currentValue("readingsSize1")
            listSizeB2 = getDevice2.currentValue("listSizeB")
            listSizeW2 = getDevice2.currentValue("listSizeW")
            maxDelay2 = getDevice2.currentValue("maxDelay")
            warnValue2 = getDevice2.currentValue("warnValue")
            lastUpdated2 = getDevice2.currentValue("lastUpdated")
        }
        if(getDevice3) {
            meanD3 = getDevice3.currentValue("meanD")
            medianD3 = getDevice3.currentValue("medianD")
            minimumD3 = getDevice3.currentValue("minimumD")
            maximumD3 = getDevice3.currentValue("maximumD")
            readingsSize3 = getDevice3.currentValue("readingsSize1")
            listSizeB3 = getDevice3.currentValue("listSizeB")
            listSizeW3 = getDevice3.currentValue("listSizeW")
            maxDelay3 = getDevice3.currentValue("maxDelay")
            warnValue3 = getDevice3.currentValue("warnValue")
            lastUpdated3 = getDevice3.currentValue("lastUpdated")
        }

        state.reportStats1 = "<table width='100%'><tr><td><b>${getDevice1}</b><br>${lastUpdated1}</td></tr><tr><td> </td></tr>"
        state.reportStats1 += "<tr><td>Number of Data Points: ${readingsSize1}<br>Over Max Threshold: ${listSizeB1}<br>Over Warning Threshold: ${listSizeW1}<br>Current Max Delay: ${maxDelay1}<br>Current Warning Delay: ${warnValue1}</td></tr><tr><td> </td></tr>"
        state.reportStats1 += "<tr><td>Mean Delay: ${meanD1}<br>Median Delay: ${medianD1}<br>Minimum Delay: ${minimumD1}<br>Maximum Delay: ${maximumD1}</td></tr></table>"

        state.reportStats2 = "<table width='100%'><tr><td><b>${getDevice2}</b><br>${lastUpdated2}</td></tr><tr><td> </td></tr>"
        state.reportStats2 += "<tr><td>Number of Data Points: ${readingsSize2}<br>Over Max Threshold: ${listSizeB2}<br>Over Warning Threshold: ${listSizeW2}<br>Current Max Delay: ${maxDelay2}<br>Current Warning Delay: ${warnValue2}</td></tr><tr><td> </td></tr>"
        state.reportStats2 += "<tr><td>Mean Delay: ${meanD2}<br>Median Delay: ${medianD2}<br>Minimum Delay: ${minimumD2}<br>Maximum Delay: ${maximumD2}</td></tr></table>"

        state.reportStats3 = "<table width='100%'><tr><td><b>${getDevice3}</b><br>${lastUpdated3}</td></tr><tr><td> </td></tr>"
        state.reportStats3 += "<tr><td>Number of Data Points: ${readingsSize3}<br>Over Max Threshold: ${listSizeB3}<br>Over Warning Threshold: ${listSizeW3}<br>Current Max Delay: ${maxDelay3}<br>Current Warning Delay: ${warnValue3}</td></tr><tr><td> </td></tr>"
        state.reportStats3 += "<tr><td>Mean Delay: ${meanD3}<br>Median Delay: ${medianD3}<br>Minimum Delay: ${minimumD3}<br>Maximum Delay: ${maximumD3}</td></tr></table>"
    }
}

def styleHandler(data){
    //if(logEnable) log.debug "In styleHandler (${state.version})"
    def colorData = ""
    if(data!=null) {
        if(data.contains(" Zw")) {
            strippedData = data.replace(" Zw","")
            def (dataZw1, dataZw2) = strippedData.split(" - ")
            colorData = "<span style='color:${colorZwav}'>${dataZw1}</span> - ${dataZw2}"
            //if(logEnable) log.debug "In styleHandler (${state.version}) - Zwav - colorData: ${colorData}"
            return colorData
        }
        if(data.contains(" Zb")) {
            strippedData = data.replace(" Zb","")
            def (dataZb1, dataZb2) = strippedData.split(" - ")
            colorData = "<span style='color:${colorZigb}'>${dataZb1}</span> - ${dataZb2}"
            //if(logEnable) log.debug "In styleHandler (${state.version}) - ZigB - colorData: ${colorData}"
            return colorData
        }
        if(data.contains(" Vt")) {
            strippedData = data.replace(" Vt","")
            def (dataV1, dataV2) = strippedData.split(" - ")
            colorData = "<span style='color:${colorVirt}'>${dataV1}</span> - ${dataV2}"
            //if(logEnable) log.debug "In styleHandler (${state.version}) - Virt - colorData: ${colorData}"
            return colorData
        }
        if(data.contains(" O")) {
            strippedData = data.replace(" O","")
            def (dataO1, dataO2) = strippedData.split(" - ")
            colorData = "<span style='color:${colorVirt}'>${dataO1}</span> - ${dataO2}"
            //if(logEnable) log.debug "In styleHandler (${state.version}) - Virt - colorData: ${colorData}"
            return colorData
        }
    }
}

def reportRawOptions(){
    dynamicPage(name: "reportRawOptions", title: "Report Raw Data", install: false, uninstall:false){
        section(getFormat("header-green", "${getImage("Blank")}"+" Report Raw Data")) {
            if(colorVirt == null) colorVirt = " - "
            if(colorZwav == null) colorZwav = " - "
            if(colorZigb == null) colorZigb = " - "
            if(colorOther == null) colorOther = " - "
            
            paragraph "Date Color Codes - Virtual (Vt): <span style='color:${colorVirt}'>${colorVirt}</span>, Zwave (Zw): <span style='color:${colorZwav}'>${colorZwav}</span>, Zigbee (Zb): <span style='color:${colorZigb}'>${colorZigb}</span>, Other (O): <span style='color:${colorOther}'>${colorOther}</span>"
            
            try {
                String result1 = state.deviceData.join(",")
                
                def data = result1.split(",")
                dataS = data.sort { a, b -> b <=> a }
                if(dataS) {
                    dataSize1 = dataS.size()
                } else {
                    dataSize1 = 0
                }
            } catch (e) {
                dataSize1 = 0
            }
            
            // Tables
            theDataPoints1 = ""
            theDataPoints2 = ""
            theDataPoints3 = ""
            theDataPoints4 = ""
            theDataPoints5 = ""
            theDataPoints6 = ""
            
            for(int i in 1..dataSize1) {
                if(i>=1 && i<=40) { theDataPoints1 += "${styleHandler(dataS[i-1])}<br>" }
                if(i>=41 && i<=80) { theDataPoints2 += "${styleHandler(dataS[i-1])}<br>" }
                if(i>=81 && i<=120) { theDataPoints3 += "${styleHandler(dataS[i-1])}<br>" }
                if(i>=121 && i<=160) { theDataPoints4 += "${styleHandler(dataS[i-1])}<br>" }
                if(i>=161 && i<=200) { theDataPoints5 += "${styleHandler(dataS[i-1])}<br>" }
                if(i>=201 && i<=240) { theDataPoints6 += "${styleHandler(dataS[i-1])}<br>" }
            }
            
            if(theDataPoints1 == "") theDataPoints1 = "No Data"
            if(theDataPoints2 == "") theDataPoints2 = "No Data"
            if(theDataPoints3 == "") theDataPoints3 = "No Data"
            if(theDataPoints4 == "") theDataPoints4 = "No Data"
            if(theDataPoints5 == "") theDataPoints5 = "No Data"
            if(theDataPoints6 == "") theDataPoints6 = "No Data"
            
            
            // Graph
            imgGraphLabelsVirt = ""
            imgGraphLabelsZigb  = ""
            imgGraphLabelsZwav = ""
            imgGraphLabelsOther = ""
            imgGraphDataVirt = ""
            imgGraphDataZigb = ""
            imgGraphDataZwav= ""
            imgGraphDataOther = ""
            
            for(int i in dataSize1..1) {
                firstSplit = dataS[i-1].split(" - ")
                graphPoint = firstSplit[1].split(" ")
                //log.warn graphPoint

                if(graphPoint!=null && graphPoint.size()>=2) {
                    graphPoint1Clean = graphPoint[0].trim()
                    if(graphPoint1Clean.contains("span")) {
                        pos = graphPoint1Clean.indexOf("'>")
                        if(pos>=0){
                           pos2 = graphPoint1Clean.indexOf("<",pos+1)
                           if(pos2>=0 && pos1<pos2) {
                              graphPoint1Clean = graphPoint1Clean.substring(pos+2,pos2)
                           }
                        }
                    }   

                    switch(graphPoint[1].trim()) { 
                        case 'Zw': 
                            imgGraphLabelsZwav += "'${graphPoint[0].trim()}',"  
                            imgGraphDataZwav += "${graphPoint1Clean},"
                            break
                        case 'Vt': 
                            imgGraphLabelsVirt += "'${graphPoint[0].trim()}',"  
                            imgGraphDataVirt += "${graphPoint1Clean},"
                            break
                        case 'Zb': 
                            imgGraphLabelsZigb += "'${graphPoint[0].trim()}',"  
                            imgGraphDataZigb += "${graphPoint1Clean},"
                            break
                        case 'O': 
                            imgGraphLabelsOther += "'${graphPoint[0].trim()}',"  
                            imgGraphDataOther += "${graphPoint1Clean},"
                            break
                    }
                }   
            }
            
            imgGrapHtmlZwav="<img width=\"100%\" src=\"https://quickchart.io/chart?c={type:'line', data:{labels:["+ imgGraphLabelsZwav +"], datasets:[{label:'Zw', data: ["+imgGraphDataZwav+"], fill:false,borderColor:'"+colorZwav+"', pointBackgroundColor:'"+colorZwav+"', pointRadius:1}]}}\">"
            imgGrapHtmlVirt="<img width=\"100%\" src=\"https://quickchart.io/chart?c={type:'line', data:{labels:["+ imgGraphLabelsVirt +"], datasets:[{label:'Vt', data: ["+imgGraphDataVirt+"], fill:false,borderColor:'"+colorVirt+"', pointBackgroundColor:'"+colorVirt+"', pointRadius:1}]}}\">"
            imgGrapHtmlZigb="<img width=\"100%\" src=\"https://quickchart.io/chart?c={type:'line', data:{labels:["+ imgGraphLabelsZigb +"], datasets:[{label:'Zb', data: ["+imgGraphDataZigb+"], fill:false,borderColor:'"+colorZigb+"', pointBackgroundColor:'"+colorZigb+"', pointRadius:1}]}}\">"
            imgGrapHtmlOther="<img width=\"100%\" src=\"https://quickchart.io/chart?c={type:'line', data:{labels:["+ imgGraphLabelsOther +"], datasets:[{label:'O', data: ["+imgGraphDataOther+"], fill:false,borderColor:'"+colorOther+"', pointBackgroundColor:'"+colorOther+"', pointRadius:1}]}}\">"
            
            report1="<table width='100%' align='center' border='1'>"
            report1+="<tr><td colspan='4'><b>Raw Data</b></a></td></tr>"
            if(imgGraphDataZwav!="") report1+="<tr><td colspan='4'><b>" + imgGrapHtmlZwav + "</b></a></td></tr>"
            if(imgGraphDataVirt!="") report1+="<tr><td colspan='4'><b>" + imgGrapHtmlVirt + "</b></a></td></tr>"
            if(imgGraphDataZigb!="") report1+="<tr><td colspan='4'><b>" + imgGrapHtmlZigb + "</b></a></td></tr>"
            if(imgGraphDataOther!="") report1+="<tr><td colspan='4'><b>" + imgGrapHtmlOther + "</b></a></td></tr>"
            
            report1+= "<tr><td width='33%'>${theDataPoints1}</td><td width='33%'>${theDataPoints2}</td><td width='33%'>${theDataPoints3}</td></tr></table>"
            report2 = "<table width='100%' align='center' border='1'><tr><td colspan='4'><b>Raw Data</b></a></td></tr>"
            report2+= "<tr><td width='33%'>${theDataPoints4}</td><td width='33%'>${theDataPoints5}</td><td width='33%'>${theDataPoints6}</td></tr></table>"
            
            paragraph "${report1}"
            paragraph "Date Color Codes - Virtual: <span style='color:${colorVirt}'>${colorVirt}</span>, Zwave: <span style='color:${colorZwav}'>${colorZwav}</span>, Zigbee: <span style='color:${colorZigb}'>${colorZigb}</span>, Other: <span style='color:${colorOther}'>${colorOther}</span>"
            paragraph "${report2}"
        }
    }
}

def appButtonHandler(buttonPressed) {
    state.whichButton = buttonPressed
    if(logEnable) log.debug "In testButtonHandler (${state.version}) - Button Pressed: ${state.whichButton}"
    if(state.whichButton == "testBtn1"){
        getSummaryData()
    }
    if(state.whichButton == "testBtn2"){
        getRawData()
    }
}
    
// ********** Normal Stuff **********

def logsOff() {
    log.info "${app.label} - Debug logging auto disabled"
    app?.updateSetting("logEnable",[value:"false",type:"bool"])
}

def checkEnableHandler() {
    state.eSwitch = false
    if(disableSwitch) { 
        if(logEnable) log.debug "In checkEnableHandler - disableSwitch: ${disableSwitch}"
        disableSwitch.each { it ->
            eSwitch = it.currentValue("switch")
            if(eSwitch == "on") { state.eSwitch = true }
        }
    }
}

def setDefaults(){
	if(logEnable == null){logEnable = false}
}

def getImage(type) {					// Modified from @Stephack Code
    def loc = "<img src=https://raw.githubusercontent.com/bptworld/Hubitat/master/resources/images/"
    if(type == "Blank") return "${loc}blank.png height=40 width=5}>"
    if(type == "checkMarkGreen") return "${loc}checkMarkGreen2.png height=30 width=30>"
    if(type == "optionsGreen") return "${loc}options-green.png height=30 width=30>"
    if(type == "optionsRed") return "${loc}options-red.png height=30 width=30>"
    if(type == "instructions") return "${loc}instructions.png height=30 width=30>"
    if(type == "logo") return "${loc}logo.png height=60>"
}

def getFormat(type, myText="") {			// Modified from @Stephack Code   
	if(type == "header-green") return "<div style='color:#ffffff;font-weight: bold;background-color:#81BC00;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
    if(type == "line") return "<hr style='background-color:#1A77C9; height: 1px; border: 0;'>"
    if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
}

def display() {
    setVersion()
    getHeaderAndFooter()
    theName = app.label
    if(theName == null || theName == "") theName = "New Child App"
    section (getFormat("title", "${getImage("logo")}" + " ${state.name} - ${theName}")) {
        paragraph "${state.headerMessage}"
		paragraph getFormat("line")
	}
}

def display2() {
	section() {
		paragraph getFormat("line")
		paragraph "<div style='color:#1A77C9;text-align:center;font-size:20px;font-weight:bold'>${state.name} - ${state.version}</div>"
        paragraph "${state.footerMessage}"
	}       
}

def getHeaderAndFooter() {
    timeSinceNewHeaders()   
    if(state.totalHours > 4) {
        //if(logEnable) log.debug "In getHeaderAndFooter (${state.version})"
        def params = [
            uri: "https://raw.githubusercontent.com/bptworld/Hubitat/master/info.json",
            requestContentType: "application/json",
            contentType: "application/json",
            timeout: 30
        ]

        try {
            def result = null
            httpGet(params) { resp ->
                state.headerMessage = resp.data.headerMessage
                state.footerMessage = resp.data.footerMessage
            }
        }
        catch (e) { }
    }
    if(state.headerMessage == null) state.headerMessage = "<div style='color:#1A77C9'><a href='https://github.com/bptworld/Hubitat' target='_blank'>BPTWorld Apps and Drivers</a></div>"
    if(state.footerMessage == null) state.footerMessage = "<div style='color:#1A77C9;text-align:center'>BPTWorld Apps and Drivers<br><a href='https://github.com/bptworld/Hubitat' target='_blank'>Donations are never necessary but always appreciated!</a><br><a href='https://paypal.me/bptworld' target='_blank'><b>Paypal</b></a></div>"
}

def timeSinceNewHeaders() { 
    if(state.previous == null) { 
        prev = new Date()
    } else {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        prev = dateFormat.parse("${state.previous}".replace("+00:00","+0000"))
    }
    def now = new Date()
    use(TimeCategory) {       
        state.dur = now - prev
        state.days = state.dur.days
        state.hours = state.dur.hours
        state.totalHours = (state.days * 24) + state.hours
    }
    state.previous = now
    //if(logEnable) log.warn "In checkHoursSince - totalHours: ${state.totalHours}"
}
