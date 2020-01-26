metadata {
    definition (name: "Denon HEOS Speaker", namespace: "dcm.heos", author: "dmeglio@gmail.com") {
		capability "AudioVolume"
		capability "MusicPlayer"
		capability "AudioNotification"
		capability "Refresh"
		capability "SpeechSynthesis"
		capability "Initialize"
		
		command "playTopResult", [[name:"Source*","type":"ENUM","description":"Source","constraints":["Rhapsody", "TuneIn", "Deezer", "Napster", "iHeartRadio", "Soundcloud", "Tidal", "Amazon Music"]],
		[name:"Type*","type":"ENUM","description":"Type","constraints":["Station", "Artist", "Album", "Track", "Playlists"]],
		[name:"Search*","type":"STRING",description:"Search"]]
    }
}

preferences {
    input "debugOutput", "bool", title: "Enable debug logging?", defaultValue: true, displayDuringSetup: false, required: false
}

def installed() {
	log.debug "Installed with settings: ${settings}"
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unschedule()
	runIn(5, initialize)
}

def uninstalled() {
	if (getDataValue("master") == "true")
	{
		telnetClose()
		parent.notifyMasterRemoved(this)
	}
}

def initialize() {
	log.debug "initializing"
	telnetClose()
	if (getDataValue("master") == "true") 
	{
		connectToHeos()
		runIn(5,loginHeosAccount)
		runIn(5, getHeosSearchCriteria)
	}
}

def getHeosSearchCriteria()
{
	state.searchCriteria = [:]
	state.browseCriteria = [:]
	sendHeosMessage("heos://browse/get_search_criteria?sid=2")
	sendHeosMessage("heos://browse/get_search_criteria?sid=3")
	sendHeosMessage("heos://browse/get_search_criteria?sid=5")
	sendHeosMessage("heos://browse/get_search_criteria?sid=6")
	sendHeosMessage("heos://browse/get_search_criteria?sid=7")
	sendHeosMessage("heos://browse/get_search_criteria?sid=9")
	sendHeosMessage("heos://browse/get_search_criteria?sid=10")
	sendHeosMessage("heos://browse/browse?sid=13")
	 
}

def connectToHeos() {
	logDebug "Connecting to HEOS"
	telnetConnect([termChars:[13,10]], getDataValue("ip"), getDataValue("port").toInteger(), null, null)
}

def parse(message) {
	def json = parseJson(message)
	logDebug "Telnet Message: ${json}"
	
	if (json.heos.result == "success" || json.heos.result == null) {
		if (json.heos.command == "player/get_players") {
			parent.distributeMessage(json.heos.command, json.payload)
			registerHeosChangeEvents()
		}
		else if (json.heos.command == "system/sign_in") {
			if (json.heos.message.startsWith("signed_in")) {
				queryHeosPlayers()
			}
		}  
		else if (json.heos.command == "event/player_now_playing_progress") {
			def payload = parseHeosQueryString(json.heos.message)
			if (payload.cur_pos.toInteger() <= 2000)
				sendHeosMessage("heos://player/get_now_playing_media?pid=${payload.pid}")
		}
		else if (json.heos.command == "player/get_now_playing_media") {
			json.payload << parseHeosQueryString(json.heos.message)
			parent.distributeMessage(json.heos.command, json.payload)
		}
		else if (json.heos.command == "player/get_mute") {
			json.payload = parseHeosQueryString(json.heos.message)
			parent.distributeMessage(json.heos.command, json.payload)
		}
		else if (json.heos.command == "player/get_volume") {
			json.payload = parseHeosQueryString(json.heos.message)
			parent.distributeMessage(json.heos.command, json.payload)
		}
		else if (json.heos.command == "player/get_play_state") {
			json.payload = parseHeosQueryString(json.heos.message)
			parent.distributeMessage(json.heos.command, json.payload)
		}
		else if (json.heos.command.startsWith("event/")) {
			if (json.heos.message != null)
				parent.distributeMessage(json.heos.command,parseHeosQueryString(json.heos.message))
		}
		else if (json.heos.command == "browse/get_search_criteria") {
			def sid = parseHeosQueryString(json.heos.message).sid.toInteger()
			state.searchCriteria[sid] = json.payload
		}
		else if (json.heos.command == "browse/browse") {
			if (!json.heos.message.startsWith("command under process")) {
				def sid = parseHeosQueryString(json.heos.message).sid.toInteger()
				state.browseCriteria[sid] = json.payload
			}
		}
	}
	else {
	}
}

def telnetStatus(message) {
	log.error "Status: ${message}"
	// Reconnect if there is a telnet error
	initialize()
}

def setMaster(isMaster) {
	updateDataValue("master", isMaster.toString())
}

def setDeviceDetails(ip, port) {
	updateDataValue("ip", ip)
	updateDataValue("port", port.toString())
}

def setPlayerId(pid) {
	updateDataValue("pid", pid.toString())
}

def sendHeosMessage(String msg) {
	if (getDataValue("master") == "true") {
		logDebug "Sending HEOS command: ${msg}"
		sendHubCommand(new hubitat.device.HubAction(msg, hubitat.device.Protocol.TELNET))
	}
	else
		parent.sendHeosMessage(msg)
}

def loginHeosAccount()
{
	def username = parent.getUsername()
	def password = parent.getPassword()
	sendHeosMessage("heos://system/sign_in?un=${username}&pw=${password}")
}

def queryHeosPlayers() {
	sendHeosMessage("heos://player/get_players")
}

def registerHeosChangeEvents() {
	sendHeosMessage("heos://system/register_for_change_events?enable=on")
}

def parseHeosQueryString(queryString) {
	def queryKvp = queryString.split("&")
	def result = [:]
	for (kvp in queryKvp) {
		def splitVals = kvp.split("=")
		result."${splitVals[0]}" = splitVals[1]
	}
	return result
}

def logDebug(msg) {
    if (settings?.debugOutput) {
		log.debug msg
	}
}


def mute() {
	def pid = getDataValue("pid")
	sendHeosMessage("heos://player/set_mute?pid=${pid}&state=on")
}

def unmute() {
	def pid = getDataValue("pid")
	sendHeosMessage("heos://player/set_mute?pid=${pid}&state=off")
}

def setVolume(volume) {
	def pid = getDataValue("pid")
	sendHeosMessage("heos://player/set_volume?pid=${pid}&level=${volume}")
}

def setLevel(volumelevel) {
	setVolume(volumelevel)
}

def volumeDown() {
	def pid = getDataValue("pid")
	sendHeosMessage("heos://player/volume_down?pid=${pid}&step=5")
}

def volumeUp() {
	def pid = getDataValue("pid")
	sendHeosMessage("heos://player/volume_up?pid=${pid}&step=5")
}

def nextTrack() {
	def pid = getDataValue("pid")
	sendHeosMessage("heos://player/play_next?pid=${pid}")
}

def previousTrack() {
	def pid = getDataValue("pid")
	sendHeosMessage(" heos://player/play_previous?pid=${pid}")
}

def play() {
	def pid = getDataValue("pid")
	sendHeosMessage("heos://player/set_play_state?pid=${pid}&state=play")
}

def pause() {
	def pid = getDataValue("pid")
	sendHeosMessage("heos://player/set_play_state?pid=${pid}&state=pause")
}

def stop() {
	def pid = getDataValue("pid")
	sendHeosMessage("heos://player/set_play_state?pid=${pid}&state=stop")
}

def playTrack(uri) {
	clearQueue()
	def pid = getDataValue("pid")
	sendHeosMessage("heos://browse/play_stream?pid=${pid}&url=${uri}")
}

def restoreTrack(uri) {
	playTrack(uri)
}

def resumeTrack(uri) {
	playTrack(uri)
}

def setTrack(uri) {
	playTrack(uri)
}

def playText(text) {
	clearQueue()
	def ttsTrack = textToSpeech(text)
	playTrack(ttsTrack.uri)
}

def playText(text, volumelevel) {
	setVolume(volumelevel)
	playText(text)
}

def playTextAndRestore(text, volumelevel) {
	playText(text, volumelevel)
}

def playTextAndResume(text, volumelevel) {
	playText(text, volumelevel)
}

def playTrack(trackuri, volumelevel) {
	setVolume(volumelevel)
	playTrack(trackuri)
}

def playTrackAndRestore(trackuri, volumelevel) {
	playTrack(trackuri, volumelevel)
}

def playTrackAndResume(trackuri, volumelevel) {
	playTrack(trackuri, volumelevel)
}

def speak(text) {
	playText(text)
}

def refresh() {
	def pid = getDataValue("pid")

	sendHeosMessage("heos://player/get_play_state?pid=${pid}")
	sendHeosMessage("heos://player/get_now_playing_media?pid=${pid}")
	sendHeosMessage("heos://player/get_volume?pid=${pid}")
	sendHeosMessage("heos://player/get_mute?pid=${pid}")
}

def playTopResult(source, type, search) {
	def sources = 
	["Rhapsody":2, "TuneIn":3, "Deezer":5, "Napster":6, "iHeartRadio":7, "Soundcloud":9, "Tidal":10, "Amazon Music":13]
	
	def sid = sources[source]
	
	if (source == "Amazon Music") {
	}
	else {
		def scid = getScidBySourceAndType(sid, type)
		if (scid != null) {
			sendHeosMessage("heos://browse/search?sid=${sid}&search=${search}&scid=${scid}")
		}
		else
			log.error "${type} search not supported by ${source}"
	}

}

def getScidBySourceAndType(sid, type) {
	for (criteria in state.searchCriteria."$sid")
	{
		if (criteria.name == type)
			return criteria.scid
	}
	return null
}

def clearQueue() {
	def pid = getDataValue("pid")
	sendHeosMessage("heos://player/clear_queue?pid=${pid}")
}