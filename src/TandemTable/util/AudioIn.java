package TandemTable.util;

import java.util.ArrayList;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import TandemTable.Colours;
import TandemTable.Sketch;
import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.AudioProcessor;
import be.hogent.tarsos.dsp.SilenceDetector;

public class AudioIn implements AudioProcessor {

	// Number of milliseconds for calculating
	// long utterance rate
	public static final int utterVisTimeLong = 120000;
	// Number of milliseconds for calculating
	// short utterance rate
	public static final int utterVisTimeShort = 5000;
	public static final float WEIGHTED_CUR = 0.75f;
	public static final float WEIGHTED_LAST = 0.25f;
	
	
	// Ratio of utter rate times
	//public static final int utterRateTimeRatio = 4;
		
	Sketch sketch;
	AudioDispatcher dispatcher;
	Mixer mixer;
	SilenceDetector silenceDetector;
	AudioFormat audioFormat;
	
	// Threshold for dB sound pressure level
	int threshold;
	//int talkingAmount = 1;
	
	// User ID
	int user;
	// Prevent both microphones from picking up 
	// same utterance data
	public boolean userUtterFlag = false;
	
	//ArrayList<SlidingWindow> windowArray;
	
	// Size of sliding window in milliseconds
	//int windowSize = 500;
	// To use sliding window approach
	//boolean slidingWindowFlag = true;
	
	ArrayList<Float> pcmData = null;
	
	// For filtering out noise
	//float noiseLevel = 0;
	//int noiseLevelIndex = 0;
	
	// Total amount of time of utterances
	long totalTimeUtterances = 0;
	// If noise profile creation is complete
	boolean noiseProfileComplete = false;
	// Time of last utterance
	long timeOfLastUtter = 0;
	// Time of last utterance for content Prompt
	long timeUtterContent = 0;
	// Last index value of drawn PCM data
	// For scrolling the drawing
	int indexScrollPCM = 0;
	// Index and counter for scrolling utterances
	int indexScrollUtter = 0;
	int sliderCounter = 0;
	// Amount of time for capturing noise
	int delayNoise = 8000;
	// Start of noise capture in milliseconds
	long startTime = 0;
	// Utterance start threshold factor
	float noiseMult = 1.05f;
	// Maximum positive noise level detected 
	float maxNoiseLvlPos = 0;
	// Maximum negative noise level detected 
	float maxNoiseLvlNeg = 0;
	// List of successful utterances
	ArrayList<Utterance> utterArray;
	// Utterance rate for number of milliseconds
	// in utterRateTimeLong
	int utterRateLong = 0;
	long utterTimeLong = 0;
	long lastUtterTimeLong = 0;
	// Utterance rate for number of milliseconds
	// in utterRateTimeLong
	int utterRateShort = 0;
	long utterTimeShort = 0;
	long lastUtterTimeShort = 0;
	// Current utterance
	Utterance curUtter = null;
	// If the utterance has started
	boolean startedUtter = false;
	// Pseudo time threshold for combining two groups of sound
	// Some words have more than one group of sound
	//int combineUtterTheshold = 200;
	int endThesholdPaper = 500;
	// Pseudo time for utterances
	int utterTime = 0;
	// If the two groups of sound should be combined
	boolean combineSounds = false;
	// Pseudo time length of utterance threshold
	//int utterLengthThresh = 400;
	int utterLengthThreshPaper = 150; //550
	// If last sound was added to the utterArray
	boolean utterAdded = false;

	
	
	public AudioIn(Mixer mixer, int user, Sketch sketch) {
		this.sketch = sketch;
		this.mixer = mixer;
		this.user = user;
		threshold = -65;//SilenceDetector.DEFAULT_SILENCE_THRESHOLD;
		//windowArray = new ArrayList<SlidingWindow>();
		pcmData = new ArrayList<Float>();
		utterArray = new ArrayList<Utterance>();
	}
	
	//public long getTimeAboveNoiseThresh() {
	//	return timeAboveNoiseThresh;
	//}
	
	//public int getTalkingAmount() {
	//	return talkingAmount;
	//}
	
	public long getTimeLastUtter() {
		return timeOfLastUtter;
	}
	
	public void setTimeLastUtter(long time) {
		timeOfLastUtter = time;
	}
	
	public long getTimeLastContent() {
		return timeUtterContent;
	}
	
	public void setTimeLastContent(long time) {
		timeUtterContent = time;
	}
	
	public ArrayList<Float> getPCMData() {
		return pcmData;
	}
	
	public int getUtterRateLong() {
		return utterRateLong;
	}
	
	public int getUtterRateShort() {
		return utterRateShort;
	}
	
	public long getUtterTimeLong() {
		return utterTimeLong;
	}
	
	public long getUtterTimeShort() {
		return utterTimeShort;
	}
	
	public long getLastUtterTimeLong() {
		return lastUtterTimeLong;
	}
	
	public long getLastUtterTimeShort() {
		return lastUtterTimeShort;
	}
	
	public void setMixer(Mixer mixer) throws LineUnavailableException,
		UnsupportedAudioFileException {
	
		if(mixer != null) {
			if(dispatcher!= null){
				dispatcher.stop();
			}
			
			float sampleRate = 44100;
			int bufferSize = 512;
			int overlap = 0;
			
			
			audioFormat = new AudioFormat(sampleRate, 16, 1, true,
					true);
			final DataLine.Info dataLineInfo = new DataLine.Info(
					TargetDataLine.class, audioFormat);
			TargetDataLine line;
			line = (TargetDataLine) mixer.getLine(dataLineInfo);
			final int numberOfSamples = bufferSize;
			line.open(audioFormat, numberOfSamples);
			line.start();
			final AudioInputStream stream = new AudioInputStream(line);
			
			// create a new dispatcher
			dispatcher = new AudioDispatcher(stream, bufferSize,
					overlap);
			
			// add a processor, handle percussion event.
			silenceDetector = new SilenceDetector(threshold, false);
			dispatcher.addAudioProcessor(silenceDetector);
			dispatcher.addAudioProcessor(this);
			
			// run the dispatcher (on a new thread).
			new Thread(dispatcher,"Audio dispatching").start();
		} else {
			System.out.println("Mixer/Input does not exist");
		}
	}
	
	public void startSoundCapture() {
		try {
			setMixer(mixer);
		} catch (LineUnavailableException e) {
			System.out.println("Microphone target data line is unavailable.");
			e.printStackTrace();
		} catch (UnsupportedAudioFileException e) {
			System.out.println("Unsupported Audio File Exception");
			e.printStackTrace();
		}
	}

	@Override
	public boolean process(AudioEvent audioEvent) {
		float[] pcmAudio = audioEvent.getFloatBuffer();
		int overlap = audioEvent.getOverlap();
		long timeNow = System.currentTimeMillis();
		
		// Detect noise levels
		if(startTime == 0) {
			startTime = timeNow;
		
		} else if(timeNow - startTime < delayNoise) {
			for(int i = overlap; i < pcmAudio.length; i++) {
				float value = pcmAudio[i];
				
				if(value < 0) { 
					//noiseLevel += pcmAudio[i];
					//noiseLevelIndex++;
					
					if(value < maxNoiseLvlNeg) {
						maxNoiseLvlNeg = value;
					}
				} else if(value > 0) {
					if(value > maxNoiseLvlPos) {
						maxNoiseLvlPos = value;
					}
				}
			}
		// Processing input
		} else if(!noiseProfileComplete) {
			System.out.println("Noise profile is complete");
			noiseProfileComplete = true;
			
			if(user == 1) {
				sketch.login.titleZone1.setTextColour(Colours.titleColor);
			} else if(user == 2) {
				sketch.login.titleZone2.setTextColour(Colours.titleColor);
			}
		}
		
		if(sketch.loggedIn && noiseProfileComplete){
			if(timeOfLastUtter == 0) {
				timeOfLastUtter = timeNow;
				timeUtterContent = timeOfLastUtter;
			}
			
			//maxNoiseLvl = noiseLevel/noiseLevelIndex;
			
			for(int i = overlap; i < pcmAudio.length; i++) {
				float value = pcmAudio[i];					
				
				//if(!userUtterFlag) {
					//detectUtterance(value, timeNow);
					detectUtterancePaper(value, timeNow);
				//}
			}
			
			determineUtterRate();
			
			sketch.audioFrame.repaint();
			
		}
		
		//handleSoundDetection();
		return true;
	}
	
	public void determineUtterRate() {
		long timeNow = System.currentTimeMillis();
		long timePast = timeNow - utterVisTimeLong;
		int numUtters = 0;
		long time = 0;
		
		for(int i = utterArray.size() - 1; i >= 0; i--) {
			if(utterArray.get(i).getEndTime() != -1 && utterArray.get(i).getEndTime() > timePast) {
				numUtters++;
				time += utterArray.get(i).getEndTime() - utterArray.get(i).getStartTime();
			}
		}
		
		utterRateLong = numUtters;
		lastUtterTimeLong = utterTimeLong;
		utterTimeLong = time;
		
		timePast = timeNow - utterVisTimeShort;
		numUtters = 0;
		time = 0;
		
		for(int i = utterArray.size() - 1; i >= 0; i--) {
			if(utterArray.get(i).getEndTime() != -1 && utterArray.get(i).getEndTime() > timePast) {
				numUtters++;
				time += utterArray.get(i).getEndTime() - utterArray.get(i).getStartTime();
			}
		}
		
		utterRateShort = numUtters;
		lastUtterTimeShort = utterTimeShort;
		utterTimeShort = time;
	}
	
	// Algorithm from 
	// 2012. Ogawa et al. Table talk enhancer: a tabletop system for enhancing and balancing mealtime conversations using utterance rates. 
	// http://doi.acm.org/10.1145/2390776.2390783
	public void detectUtterancePaper(float value, long timeNow) {
		if(!userUtterFlag) {
			if(!startedUtter && (value < maxNoiseLvlNeg*noiseMult || value > maxNoiseLvlPos*noiseMult)) {
				sketch.setAudioToggle(user);
				startedUtter = true;
				curUtter = new Utterance(utterTime, timeNow);
				curUtter.addFloat(value);
			
			} else if(startedUtter && (value < maxNoiseLvlNeg || value > maxNoiseLvlPos)) {
				curUtter.addFloat(value);
				curUtter.resetCounter();
				curUtter.setEndIndex(-1);
				curUtter.setEndTime(-1);
			} else if(startedUtter && (value >= maxNoiseLvlNeg || value <= maxNoiseLvlPos)) {			
				curUtter.addCounter(1);
				curUtter.addFloat(value);
				
				if(curUtter.getEndIndex() == -1) {
					curUtter.setEndIndex(utterTime);
					curUtter.setEndTime(timeNow);
				}
			}
			
			if(startedUtter && curUtter.getCounter() >= endThesholdPaper) {
				startedUtter = false;
				
				if(curUtter.getEndIndex() - curUtter.getStartIndex() > utterLengthThreshPaper) {
					utterArray.add(curUtter);
					totalTimeUtterances += curUtter.getEndTime() - curUtter.getStartTime();
					timeOfLastUtter = timeNow;
					timeUtterContent = timeOfLastUtter;
					sketch.setAudioToggle(user);
					
					if(user == 1) {
						sketch.logger1.log("Utterance number " + utterArray.size());
						sketch.logger1.log("Total Time of Utterances: " + totalTimeUtterances);
					} else if(user == 2) {
						sketch.logger2.log("Utterance number " + utterArray.size());
						sketch.logger2.log("Total Time of Utterances: " + totalTimeUtterances);
					}
					
					System.out.println("User: " + user + ". Utterance number " + utterArray.size() + " added and ended at " + utterTime + ". Total Time of Utterances: " + totalTimeUtterances);
				}
			}
			
			
		
		}
		pcmData.add(value);
		utterTime += 1;					
	}
	
	/*public void detectUtterance(float value, long timeNow) {
		if(value < maxNoiseLvlNeg || value > maxNoiseLvlPos) {
			
			if(!startedUtter) {
				startedUtter = true;
				//long timeNow = System.currentTimeMillis();
				
				if(curUtter == null || utterTime - curUtter.getEndIndex() >= combineUtterTheshold) {
					curUtter = new Utterance(utterTime, timeNow);
					utterAdded = false;
					//System.out.println("Utterance number " + utterArray.size() + " started at " + utterTime);
				} else {
					//System.out.println(utterTime - curUtter.getEndTime() + " " + combineUtterTheshold);
					combineSounds = true;
				}
			}
			
			if(combineSounds && utterAdded) {
				//System.out.println("Adding float to last element in utter array. Size of utterance: " + utterArray.get(utterArray.size() - 1).getPCM().size());
				utterArray.get(utterArray.size() - 1).addFloat(value);
			} else {
				curUtter.addFloat(value);
			}
			
			pcmData.add(value);
		} else {
			
			if(startedUtter) {							
				if(combineSounds && utterAdded) {
					utterArray.get(utterArray.size() - 1).setEndIndex(utterTime);
					utterArray.get(utterArray.size() - 1).setEndTime(timeNow);
					timeOfLastUtter = timeNow;
					timeUtterContent = timeOfLastUtter;
					//System.out.println("Combined sounds - Utterance number " + utterArray.size() + " ended at " + utterTime + ". Size of utterance: " + utterArray.get(utterArray.size() - 1).getPCM().size()); //System.currentTimeMillis());
					
				
				} else {
					curUtter.setEndIndex(utterTime);
					curUtter.setEndTime(timeNow);
					
					if(curUtter.getEndIndex() - curUtter.getStartIndex() > utterLengthThresh) {
						utterArray.add(curUtter);
						timeOfLastUtter = timeNow;
						timeUtterContent = timeOfLastUtter;
						utterAdded = true;
						System.out.println("Utterance number " + utterArray.size() + " added and ended at " + utterTime); //System.currentTimeMillis());
					} else {
						utterAdded = false;
					}
				}
			}
			
			startedUtter = false;
			combineSounds = false;
			pcmData.add(value);//0f);
		}
		
		utterTime += 1;				
	}*/
	
	/*public void handleSoundDetection() {
		if(slidingWindowFlag) {
			long curTime = System.currentTimeMillis();
			windowArray.add(new SlidingWindow(curTime, silenceDetector.currentSPL()));
			
			ArrayList<SlidingWindow> tempWindow = new ArrayList<SlidingWindow>(windowArray);
			//System.arraycopy(windowArray, 0, tempWindow, 0, windowArray.size());
			float windowValues = 0;
			
			for(SlidingWindow w: tempWindow) {
				if(curTime - w.time > windowSize) {
					windowArray.remove(w);
				} else {
					windowValues += w.dbSPL;
				}
			}
			
			
			
			//System.out.println(windowValues/windowIndex + " " + (System.currentTimeMillis() - timeOfLastSound > windowSize));
			
			
			if(windowValues/windowArray.size() > threshold) {
				utteranceDetected();
			}
		
		} else if(silenceDetector.currentSPL() > threshold){
			utteranceDetected();
		}
		
	}
	
	public void utteranceDetected() {
		talkingAmount++;
		timeOfLastUtter = System.currentTimeMillis();
		timeUtterContent = timeOfLastUtter;
		System.out.println("Sound detected for " + user + ": " + (int)(silenceDetector.currentSPL()) + "dB SPL, Talking Amount: " + talkingAmount);
	}*/
	
	@Override
	public void processingFinished() {
		
	}
	
	public int getUtterNumber() {
		return utterArray.size();
	}
	
	class SlidingWindow {
		long time;
		double dbSPL;
		
		public SlidingWindow(long time, double dbSPL) {
			this.time = time;
			this.dbSPL = dbSPL;
		}
	}
	
	class Utterance {
		int startIndex = -1;
		int endIndex = -1;
		long startTime = -1;
		long endTime = -1;
		ArrayList<Float> pcmData;
		int r, g, b;
		int belowThreshCounter = 0;
		
		public Utterance(int startIndex, long time) {
			this.startIndex = startIndex;
			this.startTime = time;
			pcmData = new ArrayList<Float>();
			r = (int) sketch.random(10, 256);
			g = (int) sketch.random(10, 256);
			b = (int) sketch.random(10, 256);
			
		}
		
		public ArrayList<Float> getPCM() {
			return pcmData;
		}
		
		public void addFloat(float value) {
			pcmData.add(value);
		}
		
		public void setEndIndex(int endIndex) {
			this.endIndex = endIndex;
		}
		
		public int getEndIndex() {
			return endIndex;
		}
		
		public void setEndTime(long time) {
			this.endTime = time;
		}
		
		public long getEndTime() {
			return endTime;
		}
		
		public void setStartTime(long time) {
			this.startTime = time;
		}
		
		public long getStartTime() {
			return startTime;
		}
		
		public int getStartIndex() {
			return startIndex;
		}
		
		public void addCounter(int add) {
			belowThreshCounter += add;
		}
		
		public int getCounter() {
			return belowThreshCounter;
		}
		
		public void resetCounter() {
			belowThreshCounter = 0;
		}
	}
	

}