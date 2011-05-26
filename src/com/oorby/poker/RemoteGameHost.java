package com.oorby.poker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.oorby.poker.Hand.AvailableAction;
import com.oorby.poker.Hand.HandResult;
import com.oorby.poker.Hand.PlayerHole;
import com.oorby.poker.Hand.Pot;
import com.oorby.poker.Hand.Round;
import com.oorby.poker.Hand.RoundAction;

public class RemoteGameHost implements GameHost {

	private String botName;
	private String devkey;
	private String host;
	private String lastEventId;

	public RemoteGameHost(String botName, String devkey, String host) {
		this.botName = botName;
		this.devkey = devkey;
		this.host = host.startsWith("http") ? host : "http://" + host;
	}


	@Override
	public GameEvent getNextEvent() throws Exception {
		return execGet(endpointUrl("/v1/poker/bots/" + botName + "/next_event"));
	}

	@Override
	public GameEvent takeAction(PlayerAction action) throws Exception {
		String actionCode = action.action.toCode();

		StringBuilder sb = new StringBuilder();
		sb.append("action=").append(actionCode);

		return execPost(endpointUrl("/v1/poker/bots/" + botName + "/next_event"), sb.toString());
	}


	private GameEvent execGet(String url) throws IOException, JSONException {
		URL u = new URL(url);
		HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setReadTimeout(0); // no timeout

        return strToEvent(readFromConnection(conn));
	}

	private GameEvent execPost(String url, String data) throws IOException, JSONException {
		URL u = new URL(url);
		HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setDoOutput(true);
        conn.setReadTimeout(0); // no timeout
        Writer wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(data);
        wr.flush();
        wr.close();

        return strToEvent(readFromConnection(conn));
	}

	private String readFromConnection(HttpURLConnection conn) throws IOException {
        BufferedReader in = null;
        try {
        	InputStream inputStream = (conn.getResponseCode() == 200) ? conn.getInputStream() : conn.getErrorStream();
        	in = new BufferedReader(new InputStreamReader(inputStream));
        	String line = null;
        	StringBuilder response = new StringBuilder();
        	while (null != (line = in.readLine())) { 
        		response.append(line);
        	}

        	return response.toString();
        } finally {
        	if (in != null) {
        		in.close();
        	}
        }
	}

	private String endpointUrl(String resource) {
		return addStandardArgs(host + resource);
	}

	private String addStandardArgs(String url) {
		if (!url.contains("?")) {
			url = url + "?";
		} else {
			url = url + "&";
		}

		return url + "devkey=" + devkey + "&eventId=" + lastEventId; 
	}

	private GameEvent strToEvent(String json) throws JSONException {
		JSONObject resp = new JSONObject(json);
		
		JSONObject eventJson = resp.getJSONObject("event");

		GameEvent gameEvent = new GameEvent();
		gameEvent.eventType = GameEvent.EventType.valueOf(eventJson.getString("eventType"));
		gameEvent.eventId = eventJson.getString("refId");
		gameEvent.gameState = gameStateFromEvent(eventJson);
		gameEvent.hand = handFromEvent(eventJson);

		lastEventId = gameEvent.eventId;

		return gameEvent;
	}

	// these methods are stoopid. Probably worth adding a dependency on a json deserializer to avoid this.
	private Hand handFromEvent(JSONObject eventJson) throws JSONException {
		Hand h = new Hand();
		JSONObject hand = eventJson.getJSONObject("hand");

		h.preFlop = roundFromJson("preFlop", hand);
		h.flop = roundFromJson("flop", hand);
		h.turn = roundFromJson("turn", hand);
		h.river = roundFromJson("river", hand);

		h.pots = potsFromJson(hand);
		h.handComplete = hand.getBoolean("handComplete"); 
		h.results = resultsFromJson(hand);
		h.hole = stringListFromJson(hand, "hole", "cards");
		h.communityCards = stringListFromJson(hand, "communityCards", "cards");

		h.availableActions = availableActionsFromJson(hand);
		h.showdownPlayerHoles = showdownHolesFromJson(hand);

		h.players = playersFromJson("players", hand);

		return h;
	}


	private Map<String, Player> playersFromJson(String key, JSONObject hand) throws JSONException {
		if (!hand.has(key)) {
			return null;
		}

		Map<String, Player> players = new HashMap<String, Player>(); 
		JSONArray playersJSON = hand.getJSONArray(key);
		for (int i = 0; i < playersJSON.length(); i++) {
			JSONObject playerJSON = playersJSON.getJSONObject(i);
			Player p = new Player();
			p.botName = playerJSON.getString("botName");
			p.currentStake = playerJSON.getInt("currentStake");
			players.put(p.botName, p);
		}

		return players;
	}


	private List<PlayerHole> showdownHolesFromJson(JSONObject hand) throws JSONException {
		if (!hand.has("showdownPlayerHoles")) {
			return null;
		}
		List<PlayerHole> holes = new ArrayList<Hand.PlayerHole>();
		JSONArray holesJSON = hand.getJSONArray("showdownPlayerHoles");
		for (int i = 0; i < holesJSON.length(); i++) {
			PlayerHole hole = new PlayerHole();
			JSONObject holeJSON = holesJSON.getJSONObject(i);
			hole.botName = holeJSON.getString("botName");
			hole.hole = stringListFromJson(holeJSON, "hole", "cards");
			hole.bestHand = stringListFromJson(holeJSON, "bestHand", "cards");
			holes.add(hole);
		}
		return holes;
	}


	private List<AvailableAction> availableActionsFromJson(JSONObject hand) throws JSONException {
		if (!hand.has("availableActions")) {
			return null;
		}

		List<AvailableAction> actions = new ArrayList<AvailableAction>();

		JSONArray actionsJSON = hand.getJSONArray("availableActions");
		for (int i = 0; i < actionsJSON.length(); i++) {
			AvailableAction a = new AvailableAction();
			JSONObject actionJson = actionsJSON.getJSONObject(i);
			a.action = PlayerAction.Action.fromCode(actionJson.getString("action"));
			a.costOfAction = actionJson.getInt("costOfAction");
			actions.add(a);
		}

		return actions;
	}


	private List<String> stringListFromJson(JSONObject json, String...path) throws JSONException {
		for (int i = 0; i < path.length - 1; i++) {
			if (!json.has(path[i])) {
				return null;
			}
			json = json.getJSONObject(path[i]);
		}

		JSONArray playersJSON = json.getJSONArray(path[path.length - 1]);
		List<String> strs = new ArrayList<String>(playersJSON.length());
		for (int i = 0; i < playersJSON.length(); i++) {
			strs.add(playersJSON.getString(i));
		}
		return null;
	}


	private List<HandResult> resultsFromJson(JSONObject hand) throws JSONException {
		if (!hand.has("results")) {
			return null;
		}

		List<HandResult> results = new ArrayList<Hand.HandResult>();
		JSONArray resultsJSON = hand.getJSONArray("results");
		for (int i = 0; i < resultsJSON.length(); i++) {
			JSONObject resultJSON = resultsJSON.getJSONObject(i);
			HandResult hr = new HandResult();
			hr.botName = resultJSON.getString("botName");
			hr.netStackChange = resultJSON.getInt("netStackChange");
			results.add(hr);
		}

		return results;
	}


	private List<Pot> potsFromJson(JSONObject hand) throws JSONException {
		if (!hand.has("pots")) {
			return null;
		}

		JSONArray potsJSON = hand.getJSONArray("pots");
		List<Pot> pots = new ArrayList<Pot>();

		for (int i = 0; i < potsJSON.length(); i++) {
			Pot p = new Pot();
			JSONObject potJSON = potsJSON.getJSONObject(i);
			p.amountInPot = potJSON.getInt("amountInPot");
			p.playersInPot = stringListFromJson(potJSON, "botsInPot");
		}		

		return pots;
	}

	private Round roundFromJson(String roundName, JSONObject hand) throws JSONException {
		if (!hand.has(roundName)) {
			return null;
		}
		JSONObject round = hand.getJSONObject(roundName);

		Round r = new Round();
		r.totalRoundSize = round.getInt("totalRoundSize");
		r.actions = roundActionsFromJson(round);

		return r;
	}

	private List<RoundAction> roundActionsFromJson(JSONObject round) throws JSONException {
		if (round == null || !round.has("actions")) {
			return null;
		}

		List<RoundAction> actions = new ArrayList<RoundAction>();

		JSONArray jsonArray = round.getJSONArray("actions");
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject object = (JSONObject) jsonArray.get(i);
			RoundAction action = new RoundAction();
			action.action = PlayerAction.Action.fromCode(object.getString("action"));
			action.costOfAction = object.getInt("costOfAction");
			action.botName = object.getString("botName");
			actions.add(action);
		}

		return actions;
	}

	private GameState gameStateFromEvent(JSONObject eventJson) throws JSONException {
		if (!eventJson.has("game")) {
			return null;
		}

		GameState state = new GameState();
		JSONObject stateJSON = eventJson.getJSONObject("game");
		state.gameId = stateJSON.getString("gameId");
		state.gameManagerHost = stateJSON.getString("gameManagerHost");
		state.infoUrl = stateJSON.getString("infoUrl");
		state.currentSmallBlind = stateJSON.getInt("currentSmallBlind");
		state.currentBigBlind = stateJSON.getInt("currentBigBlind");
		state.smallBet = stateJSON.getInt("smallBet");
		state.bigBet = stateJSON.getInt("bigBet");
		state.rake = stateJSON.getInt("rake");
		state.ante = stateJSON.getInt("ante");
		state.maxRaisesPerRound = stateJSON.getInt("maxRaisesPerRound");
		state.playerStakes = playersFromJson("playerStakes", stateJSON);

		return state;
	}


	public static void main(String[] args) throws JSONException {
		String results = "{\"eventId\":\"13889;1;1;5,20,17|5:c:0,20:r:40,17:r:80,5:f:0,20:r:80,17:c:40\",\"event\":{\"refId\":\"13889;1;1;5,20,17|5:c:0,20:r:40,17:r:80,5:f:0,20:r:80,17:c:40\",\"game\":{\"gameId\":\"13889\",\"gameManagerHost\":\"http://ec2-50-19-86-1.compute-1.amazonaws.com\",\"infoUrl\":\"http://www.oorby.com/matches/view/13889\",\"currentSmallBlind\":10,\"currentBigBlind\":20,\"smallBet\":20,\"bigBet\":40,\"rake\":0,\"ante\":0,\"maxRaisesPerRound\":3,\"playerStakes\":[{\"botName\":\"000006\",\"currentStake\":820},{\"botName\":\"000007\",\"currentStake\":950},{\"botName\":\"OtherBot\",\"currentStake\":760},{\"botName\":\"000009\",\"currentStake\":940},{\"botName\":\"LexingtonAv63St\",\"currentStake\":620},{\"botName\":\"LowerEastSide2Av\",\"currentStake\":620},{\"botName\":\"14St\",\"currentStake\":620},{\"botName\":\"23St\",\"currentStake\":620},{\"botName\":\"57St6Av\",\"currentStake\":280},{\"botName\":\"34StHeraldSq\",\"currentStake\":3770}]},\"hand\":{\"handNumber\":2,\"players\":[{\"botName\":\"000006\",\"currentStake\":820},{\"botName\":\"000007\",\"currentStake\":950},{\"botName\":\"OtherBot\",\"currentStake\":760},{\"botName\":\"000009\",\"currentStake\":940},{\"botName\":\"LexingtonAv63St\",\"currentStake\":620},{\"botName\":\"LowerEastSide2Av\",\"currentStake\":620},{\"botName\":\"14St\",\"currentStake\":620},{\"botName\":\"23St\",\"currentStake\":620},{\"botName\":\"57St6Av\",\"currentStake\":280},{\"botName\":\"34StHeraldSq\",\"currentStake\":3770}],\"handComplete\":true,\"hole\":{\"cards\":[\"8s\",\"Qh\"]},\"communityCards\":{\"cards\":[\"Jd\",\"Ks\",\"Ad\",\"2s\",\"3d\"]},\"showdownPlayerHoles\":[{\"botName\":\"000006\",\"hole\":{\"cards\":[\"Ac\",\"9h\"]}},{\"botName\":\"000007\",\"hole\":{\"cards\":[\"Jc\",\"8d\"]}},{\"botName\":\"OtherBot\",\"hole\":{\"cards\":[\"8s\",\"Qh\"]}},{\"botName\":\"000009\",\"hole\":{\"cards\":[\"Qc\",\"4c\"]}},{\"botName\":\"LexingtonAv63St\",\"hole\":{\"cards\":[\"4h\",\"9s\"]}},{\"botName\":\"LowerEastSide2Av\",\"hole\":{\"cards\":[\"6c\",\"5d\"]}},{\"botName\":\"14St\",\"hole\":{\"cards\":[\"3c\",\"5s\"]}},{\"botName\":\"23St\",\"hole\":{\"cards\":[\"4d\",\"2c\"]}},{\"botName\":\"57St6Av\",\"hole\":{\"cards\":[\"Js\",\"7h\"]},\"bestHand\":{\"cards\":[\"Js\",\"7h\",\"Jd\",\"Ks\",\"Ad\"]}},{\"botName\":\"34StHeraldSq\",\"hole\":{\"cards\":[\"As\",\"9d\"]},\"bestHand\":{\"cards\":[\"As\",\"9d\",\"Jd\",\"Ks\",\"Ad\"]}}],\"preFlop\":{\"actions\":[{\"botName\":\"OtherBot\",\"action\":\"b\",\"costOfAction\":10},{\"botName\":\"000009\",\"action\":\"B\",\"costOfAction\":20},{\"botName\":\"LexingtonAv63St\",\"action\":\"f\",\"costOfAction\":0},{\"botName\":\"LowerEastSide2Av\",\"action\":\"f\",\"costOfAction\":0},{\"botName\":\"14St\",\"action\":\"f\",\"costOfAction\":0},{\"botName\":\"23St\",\"action\":\"f\",\"costOfAction\":0},{\"botName\":\"57St6Av\",\"action\":\"c\",\"costOfAction\":20},{\"botName\":\"34StHeraldSq\",\"action\":\"r\",\"costOfAction\":40},{\"botName\":\"000006\",\"action\":\"c\",\"costOfAction\":40},{\"botName\":\"000007\",\"action\":\"c\",\"costOfAction\":40},{\"botName\":\"OtherBot\",\"action\":\"c\",\"costOfAction\":30},{\"botName\":\"000009\",\"action\":\"c\",\"costOfAction\":20},{\"botName\":\"57St6Av\",\"action\":\"c\",\"costOfAction\":20}],\"totalRoundSize\":240},\"flop\":{\"actions\":[{\"botName\":\"OtherBot\",\"action\":\"c\",\"costOfAction\":0},{\"botName\":\"000009\",\"action\":\"c\",\"costOfAction\":0},{\"botName\":\"57St6Av\",\"action\":\"r\",\"costOfAction\":20},{\"botName\":\"34StHeraldSq\",\"action\":\"r\",\"costOfAction\":40},{\"botName\":\"000006\",\"action\":\"r\",\"costOfAction\":60},{\"botName\":\"000007\",\"action\":\"f\",\"costOfAction\":0},{\"botName\":\"OtherBot\",\"action\":\"c\",\"costOfAction\":60},{\"botName\":\"000009\",\"action\":\"f\",\"costOfAction\":0},{\"botName\":\"57St6Av\",\"action\":\"c\",\"costOfAction\":40},{\"botName\":\"34StHeraldSq\",\"action\":\"c\",\"costOfAction\":20}],\"totalRoundSize\":240},\"turn\":{\"actions\":[{\"botName\":\"OtherBot\",\"action\":\"c\",\"costOfAction\":0},{\"botName\":\"57St6Av\",\"action\":\"r\",\"costOfAction\":40},{\"botName\":\"34StHeraldSq\",\"action\":\"r\",\"costOfAction\":80},{\"botName\":\"000006\",\"action\":\"f\",\"costOfAction\":0},{\"botName\":\"OtherBot\",\"action\":\"c\",\"costOfAction\":80},{\"botName\":\"57St6Av\",\"action\":\"r\",\"costOfAction\":80},{\"botName\":\"34StHeraldSq\",\"action\":\"c\",\"costOfAction\":40},{\"botName\":\"OtherBot\",\"action\":\"c\",\"costOfAction\":40}],\"totalRoundSize\":360},\"river\":{\"actions\":[{\"botName\":\"OtherBot\",\"action\":\"c\",\"costOfAction\":0},{\"botName\":\"57St6Av\",\"action\":\"r\",\"costOfAction\":40},{\"botName\":\"34StHeraldSq\",\"action\":\"r\",\"costOfAction\":80},{\"botName\":\"OtherBot\",\"action\":\"f\",\"costOfAction\":0},{\"botName\":\"57St6Av\",\"action\":\"r\",\"costOfAction\":80},{\"botName\":\"34StHeraldSq\",\"action\":\"c\",\"costOfAction\":40}],\"totalRoundSize\":240},\"pots\":[{\"amountInPot\":1080,\"botsInPot\":[\"34StHeraldSq\",\"000007\",\"57St6Av\",\"000009\",\"OtherBot\",\"000006\"]}],\"results\":[{\"botName\":\"000006\",\"netStackChange\":-100},{\"botName\":\"000007\",\"netStackChange\":-40},{\"botName\":\"OtherBot\",\"netStackChange\":-220},{\"botName\":\"000009\",\"netStackChange\":-40},{\"botName\":\"LexingtonAv63St\",\"netStackChange\":0},{\"botName\":\"LowerEastSide2Av\",\"netStackChange\":0},{\"botName\":\"14St\",\"netStackChange\":0},{\"botName\":\"23St\",\"netStackChange\":0},{\"botName\":\"57St6Av\",\"netStackChange\":-340},{\"botName\":\"34StHeraldSq\",\"netStackChange\":740}]},\"eventType\":\"HandComplete\"},\"events\":[{\"refId\":\"13889;1;1;5,20,17|5:c:0,20:r:40,17:r:80,5:f:0,20:r:80,17:c:40\",\"game\":{\"gameId\":\"13889\",\"gameManagerHost\":\"http://ec2-50-19-86-1.compute-1.amazonaws.com\",\"infoUrl\":\"http://www.oorby.com/matches/view/13889\",\"currentSmallBlind\":10,\"currentBigBlind\":20,\"smallBet\":20,\"bigBet\":40,\"rake\":0,\"ante\":0,\"maxRaisesPerRound\":3,\"playerStakes\":[{\"botName\":\"000006\",\"currentStake\":820},{\"botName\":\"000007\",\"currentStake\":950},{\"botName\":\"OtherBot\",\"currentStake\":760},{\"botName\":\"000009\",\"currentStake\":940},{\"botName\":\"LexingtonAv63St\",\"currentStake\":620},{\"botName\":\"LowerEastSide2Av\",\"currentStake\":620},{\"botName\":\"14St\",\"currentStake\":620},{\"botName\":\"23St\",\"currentStake\":620},{\"botName\":\"57St6Av\",\"currentStake\":280},{\"botName\":\"34StHeraldSq\",\"currentStake\":3770}]},\"hand\":{\"handNumber\":2,\"players\":[{\"botName\":\"000006\",\"currentStake\":820},{\"botName\":\"000007\",\"currentStake\":950},{\"botName\":\"OtherBot\",\"currentStake\":760},{\"botName\":\"000009\",\"currentStake\":940},{\"botName\":\"LexingtonAv63St\",\"currentStake\":620},{\"botName\":\"LowerEastSide2Av\",\"currentStake\":620},{\"botName\":\"14St\",\"currentStake\":620},{\"botName\":\"23St\",\"currentStake\":620},{\"botName\":\"57St6Av\",\"currentStake\":280},{\"botName\":\"34StHeraldSq\",\"currentStake\":3770}],\"handComplete\":true,\"hole\":{\"cards\":[\"8s\",\"Qh\"]},\"communityCards\":{\"cards\":[\"Jd\",\"Ks\",\"Ad\",\"2s\",\"3d\"]},\"showdownPlayerHoles\":[{\"botName\":\"000006\",\"hole\":{\"cards\":[\"Ac\",\"9h\"]}},{\"botName\":\"000007\",\"hole\":{\"cards\":[\"Jc\",\"8d\"]}},{\"botName\":\"OtherBot\",\"hole\":{\"cards\":[\"8s\",\"Qh\"]}},{\"botName\":\"000009\",\"hole\":{\"cards\":[\"Qc\",\"4c\"]}},{\"botName\":\"LexingtonAv63St\",\"hole\":{\"cards\":[\"4h\",\"9s\"]}},{\"botName\":\"LowerEastSide2Av\",\"hole\":{\"cards\":[\"6c\",\"5d\"]}},{\"botName\":\"14St\",\"hole\":{\"cards\":[\"3c\",\"5s\"]}},{\"botName\":\"23St\",\"hole\":{\"cards\":[\"4d\",\"2c\"]}},{\"botName\":\"57St6Av\",\"hole\":{\"cards\":[\"Js\",\"7h\"]},\"bestHand\":{\"cards\":[\"Js\",\"7h\",\"Jd\",\"Ks\",\"Ad\"]}},{\"botName\":\"34StHeraldSq\",\"hole\":{\"cards\":[\"As\",\"9d\"]},\"bestHand\":{\"cards\":[\"As\",\"9d\",\"Jd\",\"Ks\",\"Ad\"]}}],\"preFlop\":{\"actions\":[{\"botName\":\"OtherBot\",\"action\":\"b\",\"costOfAction\":10},{\"botName\":\"000009\",\"action\":\"B\",\"costOfAction\":20},{\"botName\":\"LexingtonAv63St\",\"action\":\"f\",\"costOfAction\":0},{\"botName\":\"LowerEastSide2Av\",\"action\":\"f\",\"costOfAction\":0},{\"botName\":\"14St\",\"action\":\"f\",\"costOfAction\":0},{\"botName\":\"23St\",\"action\":\"f\",\"costOfAction\":0},{\"botName\":\"57St6Av\",\"action\":\"c\",\"costOfAction\":20},{\"botName\":\"34StHeraldSq\",\"action\":\"r\",\"costOfAction\":40},{\"botName\":\"000006\",\"action\":\"c\",\"costOfAction\":40},{\"botName\":\"000007\",\"action\":\"c\",\"costOfAction\":40},{\"botName\":\"OtherBot\",\"action\":\"c\",\"costOfAction\":30},{\"botName\":\"000009\",\"action\":\"c\",\"costOfAction\":20},{\"botName\":\"57St6Av\",\"action\":\"c\",\"costOfAction\":20}],\"totalRoundSize\":240},\"flop\":{\"actions\":[{\"botName\":\"OtherBot\",\"action\":\"c\",\"costOfAction\":0},{\"botName\":\"000009\",\"action\":\"c\",\"costOfAction\":0},{\"botName\":\"57St6Av\",\"action\":\"r\",\"costOfAction\":20},{\"botName\":\"34StHeraldSq\",\"action\":\"r\",\"costOfAction\":40},{\"botName\":\"000006\",\"action\":\"r\",\"costOfAction\":60},{\"botName\":\"000007\",\"action\":\"f\",\"costOfAction\":0},{\"botName\":\"OtherBot\",\"action\":\"c\",\"costOfAction\":60},{\"botName\":\"000009\",\"action\":\"f\",\"costOfAction\":0},{\"botName\":\"57St6Av\",\"action\":\"c\",\"costOfAction\":40},{\"botName\":\"34StHeraldSq\",\"action\":\"c\",\"costOfAction\":20}],\"totalRoundSize\":240},\"turn\":{\"actions\":[{\"botName\":\"OtherBot\",\"action\":\"c\",\"costOfAction\":0},{\"botName\":\"57St6Av\",\"action\":\"r\",\"costOfAction\":40},{\"botName\":\"34StHeraldSq\",\"action\":\"r\",\"costOfAction\":80},{\"botName\":\"000006\",\"action\":\"f\",\"costOfAction\":0},{\"botName\":\"OtherBot\",\"action\":\"c\",\"costOfAction\":80},{\"botName\":\"57St6Av\",\"action\":\"r\",\"costOfAction\":80},{\"botName\":\"34StHeraldSq\",\"action\":\"c\",\"costOfAction\":40},{\"botName\":\"OtherBot\",\"action\":\"c\",\"costOfAction\":40}],\"totalRoundSize\":360},\"river\":{\"actions\":[{\"botName\":\"OtherBot\",\"action\":\"c\",\"costOfAction\":0},{\"botName\":\"57St6Av\",\"action\":\"r\",\"costOfAction\":40},{\"botName\":\"34StHeraldSq\",\"action\":\"r\",\"costOfAction\":80},{\"botName\":\"OtherBot\",\"action\":\"f\",\"costOfAction\":0},{\"botName\":\"57St6Av\",\"action\":\"r\",\"costOfAction\":80},{\"botName\":\"34StHeraldSq\",\"action\":\"c\",\"costOfAction\":40}],\"totalRoundSize\":240},\"pots\":[{\"amountInPot\":1080,\"botsInPot\":[\"34StHeraldSq\",\"000007\",\"57St6Av\",\"000009\",\"OtherBot\",\"000006\"]}],\"results\":[{\"botName\":\"000006\",\"netStackChange\":-100},{\"botName\":\"000007\",\"netStackChange\":-40},{\"botName\":\"OtherBot\",\"netStackChange\":-220},{\"botName\":\"000009\",\"netStackChange\":-40},{\"botName\":\"LexingtonAv63St\",\"netStackChange\":0},{\"botName\":\"LowerEastSide2Av\",\"netStackChange\":0},{\"botName\":\"14St\",\"netStackChange\":0},{\"botName\":\"23St\",\"netStackChange\":0},{\"botName\":\"57St6Av\",\"netStackChange\":-340},{\"botName\":\"34StHeraldSq\",\"netStackChange\":740}]},\"eventType\":\"HandComplete\"}]}";

		RemoteGameHost h = new RemoteGameHost("", "", "");
		GameEvent ev = h.strToEvent(results);
		System.out.println(ev);
	}
}
