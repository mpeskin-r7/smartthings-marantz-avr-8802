/**
 *  Marantz Network Receiver
 *    Works on Marantz AVR 8802
 *    Based on Denon/Marantz receiver by Kristopher Kubicki & Sean Buckley
 *    SmartThings driver to connect your Marantz Preprocessor to SmartThings
 *
 */
 
preferences {
    input("destIp", "text", title: "IP", description: "The device IP")
    input("destPort", "number", title: "Port", description: "The port you wish to connect", defaultValue: 80)
    input("directInput1", "text", title: "Direct Input 1", description: "Input Channel for direct input 1", defaultValue: "SAT/CBL")
    input("directInput2", "text", title: "Direct Input 2", description: "Input Channel for direct input 2", defaultValue: "TV")
    input("directInput3", "text", title: "Direct Input 3", description: "Input Channel for direct input 3", defaultValue: "BD")
}
 

metadata {
    definition (name: "Marantz AVR 8802", namespace: "mpeskin-r7",
        author: "Sean Buckley / Kristopher Kubicki / Mark Peskin") {
        capability "Actuator"
        capability "Switch" 
        capability "Polling"
        capability "Switch Level"
        
        attribute "mute", "string"
        attribute "input", "string"
        attribute "input1", "string"
        attribute "input2", "string"
        attribute "input3", "string"
        attribute "inputChanCanonical", "enum"
        attribute "inputChanFriendly", "enum"
        attribute "canonicalInput", "string"

        command "mute"
        command "unmute"
        command "inputSelect", ["string"] //define that inputSelect takes a string of the input name as a parameter
        command "inputNext"
        command "toggleMute"
        command "input1"
        command "input2"
        command "input3"
    
        }

    simulator {
        // TODO-: define status and reply messages here
    }

    tiles {
    
    
        standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: false, canChangeBackground: true) {
            state "on", label: '${name}', action:"switch.off", backgroundColor: "#79b821", icon:"st.Electronics.electronics16"
            state "off", label: '${name}', action:"switch.on", backgroundColor: "#ffffff", icon:"st.Electronics.electronics16"
        }
        standardTile("poll", "device.poll", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, decoration: "flat", canChangeBackground: false) {
            state "poll", label: "", action: "polling.poll", icon: "st.secondary.refresh", backgroundColor: "#FFFFFF"
        }
        standardTile("input", "device.input", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, decoration: "flat", canChangeBackground: false) {
            state "input", label: '${currentValue}', action: "inputNext", icon: "", backgroundColor: "#FFFFFF"
        }
        standardTile("input1", "device.input1", width: 1, height: 1, canChangeIcon: true, inactiveLabel: true, decoration: "flat", canChangeBackground: false) {
        	state "input", label: '${currentValue}', action: "input1", icon: "", backgroundColor: "#FFFFFF"
       	}
        standardTile("input2", "device.input2", width: 1, height: 1, canChangeIcon: true, inactiveLabel: true, decoration: "flat", canChangeBackground: false) {
        	state "input", label: '${currentValue}', action: "input2", icon: "", backgroundColor: "#FFFFFF"
       	}
        standardTile("input3", "device.input3", width: 1, height: 1, canChangeIcon: true, inactiveLabel: true, decoration: "flat", canChangeBackground: false) {
        	state "input", label: '${currentValue}', action: "input3", icon: "", backgroundColor: "#FFFFFF"
       	}
        standardTile("mute", "device.mute", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, decoration: "flat", canChangeBackground: false) {
            state "muted", label: '${name}', action:"unmute", backgroundColor: "#79b821", icon:"st.Electronics.electronics13"
            state "unmuted", label: '${name}', action:"mute", backgroundColor: "#ffffff", icon:"st.Electronics.electronics13"
        }
        controlTile("level", "device.level", "slider", height: 1, width: 2, inactiveLabel: false, range: "(-80..10)") {
            state "level", label: '${name}', action:"setLevel"
        }
    
        main "switch"
        details(["switch","input","mute","level","poll","input1","input2","input3"])
    }
}



def parse(String description) {
   
    //log.debug("Entering parse")
    sendEvent(name: "input1", value: directInput1)
    sendEvent(name: "input2", value: directInput2)
    sendEvent(name: "input3", value: directInput3)
    
    def map = stringToMap(description)
  
    if(!map.body || map.body == "DQo=") { return }
        //log.debug "${map.body} "
    def body = new String(map.body.decodeBase64())

    def statusrsp = new XmlSlurper().parseText(body)

    def power = statusrsp.Power.value.text()
    if(power == "ON") { 
        sendEvent(name: "switch", value: 'on')
    } else {
        sendEvent(name: "switch", value: 'off')
    }
    
    def muteLevel = statusrsp.Mute.value.text()
    if(muteLevel == "on") { 
        sendEvent(name: "mute", value: 'muted')
    } else {
        sendEvent(name: "mute", value: 'unmuted')
    }
    
    def currentInputFunc = statusrsp.InputFuncSelect.value.text()
    def currentNetFunc = statusrsp.NetFuncSelect.value.text()

    // If NetFuncSelect exists, we're parsing formMainZone_MainZoneXml, which has the "friendly" input name
    // If InputFunc is "NET", use the value in NetFunc instead.
    if(currentNetFunc != "") {
        if(currentInputFunc == "NET") {
            sendEvent(name: "input", value: currentNetFunc)
        } else {
            sendEvent(name: "input", value: currentInputFunc)
        }
    } else {
        // Presumably we're parsing formMainZone_MainZoneXmlStatus
        // Pick off the canonical input name
        sendEvent(name: "canonicalInput", value: currentInputFunc)

        //Let's try to parse the current set of active inputs and custom naming
        // A hard coded mapping of the channel names listed in status to the channel names needed to actually issue commands
        // Why are these different? Ask someone at Marantz...
        def channelMap = ['CBL/SAT':'SAT/CBL','Blu-ray':'BD','Media Player':'MPLAY','iPod/USB':'USB/IPOD','TV AUDIO':'TV','Bluetooth':'BT']
        
        def inputFunctionList = statusrsp.InputFuncList.value
        def inputFriendlyNameList = statusrsp.RenameSource.value
        def inputSourceDelete = statusrsp.SourceDelete.value
        def inputFriendlyNames = []
        def inputCanonicalNames = []

        def inputCounter = 0

        inputFunctionList.each {
            if (inputSourceDelete[inputCounter] == 'USE') {
            	def candidateInput = (String)inputFunctionList[inputCounter]
                if (channelMap.containsKey(candidateInput)) {
                	inputCanonicalNames.add(channelMap[candidateInput])
                } else {
                	inputCanonicalNames.add(candidateInput)
                }
                def theFriendlyName = (String)inputFriendlyNameList[inputCounter].value
                inputFriendlyNames.add(theFriendlyName.trim())
            }
            inputCounter++
        }
        // log.debug("Updating canonincal channel names to $inputCanonicalNames")
        sendEvent(name: "inputChanCanonical", value: inputCanonicalNames)
        // log.debug("Updating friendly channel names to $inputFriendlyNames")
        sendEvent(name: "inputChanFriendly", value: inputFriendlyNames)

    }

    if(statusrsp.MasterVolume.value.text() != "") {
        def int volLevel = (int) statusrsp.MasterVolume.value.toFloat() ?: -35.0
        //log.debug("Read volume level $volLevel")
        def int curLevel = -35
        try {
            curLevel = device.currentValue("level")
        } catch(Exception nfe) {
            curLevel = -35
        }

        if(curLevel != volLevel) {
            //log.debug("Sending volume level $volLevel")
            sendEvent(name: "level", value: volLevel)
        }
    }

}


def setLevel(val) {
    sendEvent(name: "mute", value: "unmuted")     
    sendEvent(name: "level", value: val)
    request("cmd0=PutMasterVolumeSet/$val")
}

def on() {
    sendEvent(name: "switch", value: 'on')
    request('cmd0=PutSystem_OnStandby/ON')
}

def off() { 
    sendEvent(name: "switch", value: 'off')
    request('cmd0=PutSystem_OnStandby/STANDBY')
}

def mute() { 
    sendEvent(name: "mute", value: "muted")
    request('cmd0=PutVolumeMute/ON')
}

def unmute() { 
    sendEvent(name: "mute", value: "unmuted")
    request('cmd0=PutVolumeMute/OFF')
}

def toggleMute(){
    if(device.currentValue("mute") == "muted") { unmute() }
    else { mute() }
}

def inputNext() { 

    def cur = device.currentValue("canonicalInput")
    def selectedInputs = device.currentValue("inputChanCanonical").substring(1,device.currentValue("inputChanCanonical").length()-1).split(', ').collect{it}
    selectedInputs.push(selectedInputs[0])
    log.debug "CURRENT: $cur SELECTED: $selectedInputs"
    
    def semaphore = 0
    for(selectedInput in selectedInputs) {
        if(semaphore == 1) { 
          log.debug "SELECT: ($semaphore) '$selectedInput'"
            return inputSelect(selectedInput)
        }
        if(cur == selectedInput) { 
            semaphore = 1
        }
    }
    return inputSelect(selectedInputs[0])
}


def inputSelect(channel) {
    sendEvent(name: "canonicalInput", value: channel )
    sendEvent(name: "input", value: convertToFriendlyInputName(channel))
    //def cmdChannel = java.net.URLEncoder.encode(channel, "UTF-8")
    log.debug("Selecting input cmd0=PutZone_InputFunction/$channel")
    request("cmd0=PutZone_InputFunction/$channel")
}

def input1() {
	sendEvent(name: "input1", value: directInput1)
    return inputSelect(convertToCanonicalInputName(directInput1))
}

def input2() {
	sendEvent(name: "input2", value: directInput2)
    return inputSelect(convertToCanonicalInputName(directInput2))
}

def input3() {
	sendEvent(name: "input3", value: directInput3)
    return inputSelect(convertToCanonicalInputName(directInput3))
}

def poll() { 
    refresh()
}

def refresh() {

    def hosthex = convertIPtoHex(destIp)
    def porthex = convertPortToHex(destPort)
    device.deviceNetworkId = "$hosthex:$porthex" 

    def hubAction = [new physicalgraph.device.HubAction(
            'method': 'GET',
            'path': "/goform/formMainZone_MainZoneXmlStatus.xml",
            'headers': [ HOST: "$destIp:$destPort" ] 
        ), getfunction()]   
    
    hubAction
}

// Get function calls the XML page that shows both input and net function
def getfunction() {

    def hosthex = convertIPtoHex(destIp)
    def porthex = convertPortToHex(destPort)
    device.deviceNetworkId = "$hosthex:$porthex" 

    def hubAction = new physicalgraph.device.HubAction(
            'method': 'GET',
            'path': "/goform/formMainZone_MainZoneXml.xml",
            'headers': [ HOST: "$destIp:$destPort" ] 
        ) 

    hubAction
}

def request(body) { 

    def hosthex = convertIPtoHex(destIp)
    def porthex = convertPortToHex(destPort)
    device.deviceNetworkId = "$hosthex:$porthex" 

    def hubAction = new physicalgraph.device.HubAction(
            'method': 'POST',
            'path': "/MainZone/index.put.asp",
            'body': body,
            'headers': [ HOST: "$destIp:$destPort" ]
        ) 

    hubAction
}


private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02X', it.toInteger() ) }.join()
    return hex
}

private String convertPortToHex(port) {
    String hexport = port.toString().format( '%04X', port.toInteger() )
    return hexport
}

//Try to convert a canonical input to the corresponding friendly input
private String convertToFriendlyInputName(canonicalName) {
	def canonicalInputs = device.currentValue("inputChanCanonical").substring(1,device.currentValue("inputChanCanonical").length()-1).split(', ').collect{it}
	def friendlyInputs = device.currentValue("inputChanFriendly").substring(1,device.currentValue("inputChanFriendly").length()-1).split(', ').collect{it}
    def converted = (String)canonicalName
    
    def index = 0
    for(candidateName in canonicalInputs) {
    	 if ((String)canonicalName == (String)candidateName) {
         	converted = (String)friendlyInputs[index]
            break
         }
         index++
    }
    return converted
}

//Try to convert a friendly input to the corresponding canonical input
private String convertToCanonicalInputName(friendlyName) {
	def canonicalInputs = device.currentValue("inputChanCanonical").substring(1,device.currentValue("inputChanCanonical").length()-1).split(', ').collect{it}
	def friendlyInputs = device.currentValue("inputChanFriendly").substring(1,device.currentValue("inputChanFriendly").length()-1).split(', ').collect{it}
    def converted = (String)friendlyName
    
    def index = 0
    for(candidateName in friendlyInputs) {
    	 if ((String)friendlyName == (String)candidateName) {
         	converted = (String)canonicalInputs[index]
            break
         }
         index++
    }
    return converted
}

def updated() {

    sendEvent(name: "input1", value: directInput1)
    sendEvent(name: "input2", value: directInput2)
    sendEvent(name: "input3", value: directInput3)

    unschedule(poll)
    runIn(2,poll)
    runEvery1Minute(poll)
}