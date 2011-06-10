package com.oorby.poker;

public class PlayerAction {

	public enum Action {
		Fold, Call, Raise;

		public String toCode() {
			return toCode(this);
		}

		public static String toCode(Action action) {
			switch (action) {
			case Fold:
				return "f";

			case Call: 
				return "c";

			case Raise: 
				return "r";
			}

			return null;
		}

		public static Action fromCode(String code) {
			switch (code.charAt(0)) {
				case 'f': return Fold;
				case 'c': return Call;
				case 'r': return Raise;
			}

			return null;
		}
	}


	public Action action;
	// private int amount;


	public PlayerAction(Action action) {
		this.action = action;
	}


	public String toString() {
		return "PlayerAction [action=" + action + "]";
	}
}
