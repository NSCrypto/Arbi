package com.tsavo.trade;

import java.beans.PropertyVetoException;
import java.util.Locale;

import javax.speech.AudioException;
import javax.speech.Central;
import javax.speech.EngineException;
import javax.speech.EngineStateError;
import javax.speech.synthesis.Synthesizer;
import javax.speech.synthesis.SynthesizerModeDesc;
import javax.speech.synthesis.Voice;

public class SpeechSynthesizer {
	SynthesizerModeDesc desc;
	Synthesizer synthesizer;
	Voice voice;

	public SpeechSynthesizer() {
		try {
			init();
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public void init() throws EngineException, AudioException, EngineStateError, PropertyVetoException {
		if (desc == null) {
			System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
			desc = new SynthesizerModeDesc(Locale.US);
			Central.registerEngineCentral("com.sun.speech.freetts.jsapi.FreeTTSEngineCentral");
			synthesizer = Central.createSynthesizer(desc);
			synthesizer.allocate();
			synthesizer.resume();
			SynthesizerModeDesc smd = (SynthesizerModeDesc) synthesizer.getEngineModeDesc();
			Voice[] voices = smd.getVoices();
			for (int i = 0; i < voices.length; i++) {
				if (voices[i].getName().equals("kevin16")) {
					voice = voices[i];
					break;
				}
			}
			synthesizer.getSynthesizerProperties().setVoice(voice);
		}
	}

	public void terminate() throws EngineException, EngineStateError {
		synthesizer.deallocate();
	}

	public void speak(String speakText) throws EngineException, AudioException, IllegalArgumentException, InterruptedException {
		synthesizer.speakPlainText(speakText, null);
		// synthesizer.waitEngineState(Synthesizer.QUEUE_EMPTY);
	}
}
