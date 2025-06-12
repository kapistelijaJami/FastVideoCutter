package fastvideocutter;

public enum AcceptedType {
	MP4("VIDEO", true, true), MKV("VIDEO", true, true), WAV("AUDIO", false, false), MP3("AUDIO", false, false), M4A("AUDIO", true, true), FLAC("AUDIO", false, false);
	
	private final String mediaType;
	private final boolean supportsMultipleStreams;
	private final boolean supportsCopy; //True if supports copy codec always, otherwise let ffmpeg decide how to encode it (it should usually default to copy if possible).
	
	private AcceptedType(String mediaType, boolean supportsMultipleStreams, boolean supportsCopy) {
		this.mediaType = mediaType;
		this.supportsMultipleStreams = supportsMultipleStreams;
		this.supportsCopy = supportsCopy;
	}
	
	public String getMediaType() {
		return mediaType;
	}
	
	public boolean isVideo() {
		return this.mediaType.equals("VIDEO");
	}
	
	public boolean isAudio() {
		return this.mediaType.equals("AUDIO");
	}
	
	public boolean supportsMultipleStreams() {
		return this.supportsMultipleStreams;
	}
	
	public boolean supportsCopy() {
		return this.supportsCopy;
	}
	
	public static AcceptedType endsWithAcceptedType(String filename) {
		for (AcceptedType acceptedType : AcceptedType.values()) {
			if (acceptedType.equals(filename)) {
				return acceptedType;
			}
		}
		return null;
	}
	
	public boolean equals(String filename) { //Filename = file.mp4 etc. or just type: mp4 etc.
		int i = filename.lastIndexOf(".");
		
		if (i == -1) {
			return equalsType(filename);
		}
		
		return filename.substring(i + 1).toLowerCase().equals(this.toString());
	}
	
	public boolean equalsType(String type) { //Type = mp4 etc.
		return type.toLowerCase().equals(this.toString());
	}
	
	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
}
