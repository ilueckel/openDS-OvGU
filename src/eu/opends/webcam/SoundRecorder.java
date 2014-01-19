package eu.opends.webcam;

import java.io.*;
import javax.sound.sampled.*;

public class SoundRecorder implements Runnable {

	// path of the wav file
	File wavFile;

	public Thread thread;

	String outputFolder = "";

	// format of audio file
	AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;

	// the line from which audio data is captured
	TargetDataLine line;

	public SoundRecorder(String OutputFolder) {
		outputFolder = OutputFolder;
		wavFile = new File(outputFolder + "\\RecordedAudio.wav");
	}

	/**
	 * Defines an audio format
	 */
	AudioFormat getAudioFormat() {
		float sampleRate = 44100;
		int sampleSizeInBits = 16;
		int channels = 2;
		boolean signed = true;
		boolean bigEndian = true;
		AudioFormat format = new AudioFormat(sampleRate, sampleSizeInBits,
				channels, signed, bigEndian);
		return format;
	}

	/**
	 * Captures the sound and record into a WAV file
	 */
	public void start() {
		thread = new Thread(this);
		thread.setName("Capture Mic");
		thread.start();
	}

	/**
	 * Closes the target data line to finish capturing and recording
	 */
	public void finish() {
		thread = null;
		line.stop();
		line.close();
		System.out.println("Finished");
	}

	@Override
	public void run() {
		try {
			AudioFormat format = getAudioFormat();
			DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

			// checks if system supports the data line
			if (!AudioSystem.isLineSupported(info)) {
				System.out.println("Line not supported");
				thread = null;
			}
			line = (TargetDataLine) AudioSystem.getLine(info);
			line.open(format);
			line.start(); // start capturing

			System.out.println("Start capturing...");

			AudioInputStream ais = new AudioInputStream(line);

			System.out.println("Start recording...");

			// start recording
			AudioSystem.write(ais, fileType, wavFile);

		} catch (LineUnavailableException ex) {
			ex.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
