package TandemTable.util;

import java.util.ArrayList;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import TandemTable.Sketch;
import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.AudioProcessor;
import be.hogent.tarsos.dsp.SilenceDetector;

public class AudioIn implements AudioProcessor {

	Sketch sketch;
	AudioDispatcher dispatcher;
	Mixer mixer;
	SilenceDetector silenceDetector;
	int threshold;
	public int talkingAmount = 1;
	int user;
	long timeOfLastUtter = 0;
	ArrayList<SlidingWindow> windowArray;
	
	// Size of sliding window in milliseconds
	int windowSize = 500;
	// To use sliding window approach
	boolean slidingWindowFlag = true;
	
	ArrayList<Float> pcmData = null;
	
	// For filtering out noise
	//float noiseLevel = 0;
	//int noiseLevelIndex = 0;
	int delayNoise = 3000;
	long startTime = 0;
	
	// Maximum positive noise level detected 
	float maxNoiseLvlPos = 0;
	// Maximum negative noise level detected 
	float maxNoiseLvlNeg = 0;
	// List of successful utterances
	ArrayList<Utterance> utterArray;
	// Current utterance
	Utterance curUtter = null;
	// If the utterance has started
	boolean startedUtter = false;
	// Pseudo time threshold for combining two groups of sound
	// Some words have more than one group of sound
	int combineUtterTheshold = 200;
	// Pseudo time for utterances
	int utterTime = 0;
	// If the two groups of sound should be combined
	boolean combineSounds = false;
	// Pseudo time length of utterance threshold
	int utterLengthThresh = 400;
	// If last sound was added to the utterArray
	boolean utterAdded = false;

	
	
	public AudioIn(Mixer mixer, int user, Sketch sketch) {
		this.sketch = sketch;
		this.mixer = mixer;
		this.user = user;
		threshold = -65;//SilenceDetector.DEFAULT_SILENCE_THRESHOLD;
		windowArray = new ArrayList<SlidingWindow>();
		pcmData = new ArrayList<Float>();
		utterArray = new ArrayList<Utterance>();
	}
	
	
	
	public void setMixer(Mixer mixer) throws LineUnavailableException,
		UnsupportedAudioFileException {
	
		if(dispatcher!= null){
			dispatcher.stop();
		}
		
		float sampleRate = 44100;
		int bufferSize = 512;
		int overlap = 0;
		
		
		final AudioFormat format = new AudioFormat(sampleRate, 16, 1, true,
				true);
		final DataLine.Info dataLineInfo = new DataLine.Info(
				TargetDataLine.class, format);
		TargetDataLine line;
		line = (TargetDataLine) mixer.getLine(dataLineInfo);
		final int numberOfSamples = bufferSize;
		line.open(format, numberOfSamples);
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
	}
	
	public void draw() {
		sketch.background(0);
		sketch.stroke(255);
		sketch.strokeWeight(1);
		ArrayList<Float> tempData = new ArrayList<Float>(pcmData);
		
		float yHeight = sketch.height/2;
		float lastX = 0, lastY = yHeight;
		float mult = 2000;
		float index = 0;
		float indexAdd = (float) 0.01;
		
		for(int i = 0; i < tempData.size(); i++) {
			float sample = tempData.get(i);
			
			//System.out.println(sample + " " + index);
			sketch.line(lastX, lastY, index, yHeight + sample*mult);
			lastX = index;
			lastY = yHeight + sample*mult;
			//sketch.ellipse(index, sketch.height/2 + sample*mult, 0.1f, 0.1f);
			index += indexAdd;
			
			/*if(index > sketch.getWidth()) {
				yHeight = lastY = (float) (yHeight*1.5);
				index = lastX = 0;
			}*/
		}
		
		sketch.textSize(22);
		
		// Draw thresholds
		sketch.stroke(255, 0, 0);
		sketch.fill(255, 0, 0);
		float heightY = (float) ((sketch.height/2)*0.9);
		float x = 100;
		float length = x + utterLengthThresh*indexAdd;
		sketch.line(x, heightY, length, heightY);
		sketch.text("Min Utterance Length", length, (float) (heightY*0.99));
		
		sketch.stroke(255, 255, 255);
		sketch.fill(255, 255, 255);
		heightY = (float) ((sketch.height/2)*0.95);
		length = x + combineUtterTheshold*indexAdd;
		sketch.line(x, heightY, length, heightY);
		sketch.text("Combined Utter Length", length, (float) (heightY*0.99));
		
		sketch.stroke(0, 0, 255);
		heightY = sketch.height/2 + maxNoiseLvlPos*mult;
		sketch.line(0, heightY, sketch.getWidth(), heightY);
		
		heightY = sketch.height/2 + maxNoiseLvlNeg*mult;
		sketch.line(0, heightY, sketch.getWidth(), heightY);
		
		
		// Draw captured utterances
		ArrayList<Utterance> tempUtter = new ArrayList<Utterance>(utterArray);
		float y = (float) ((sketch.height/2)*0.75);
		
		for(int i = 0; i < tempUtter.size(); i++) {
			Utterance utterance = tempUtter.get(i);
			
			sketch.stroke(utterance.r, utterance.g, utterance.b);
			
			sketch.line(utterance.startTime*indexAdd, y, utterance.endTime*indexAdd, y);
			
			
			lastX = utterance.getStartTime()*indexAdd;
			index = lastX;
			float mainY = (float) (sketch.height/1.5);
			lastY = mainY;
			
			ArrayList<Float> tempPCM = new ArrayList<Float>(utterance.getPCM());
			
			for(Float f: tempPCM) {
				sketch.line(lastX, lastY, index, mainY + f.floatValue()*mult);
				lastX = index;
				lastY = mainY + f.floatValue()*mult;
				index += indexAdd;
				
			}
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
		
		if(startTime == 0) {
			startTime = System.currentTimeMillis();
		
		} else if(System.currentTimeMillis() - startTime < delayNoise) {
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
		} else {
			//maxNoiseLvl = noiseLevel/noiseLevelIndex;
			
			for(int i = overlap; i < pcmAudio.length; i++) {
				float value = pcmAudio[i];					
					
					if(value < maxNoiseLvlNeg || value > maxNoiseLvlPos) {
						
						if(!startedUtter) {
							startedUtter = true;
							//long timeNow = System.currentTimeMillis();
							
							if(curUtter == null || utterTime - curUtter.getEndTime() >= combineUtterTheshold) {
								curUtter = new Utterance(utterTime);
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
								utterArray.get(utterArray.size() - 1).setEndTime(utterTime);
								//System.out.println("Combined sounds - Utterance number " + utterArray.size() + " ended at " + utterTime + ". Size of utterance: " + utterArray.get(utterArray.size() - 1).getPCM().size()); //System.currentTimeMillis());
								
							
							} else {
								curUtter.setEndTime(utterTime);
								
								if(curUtter.getEndTime() - curUtter.getStartTime() > utterLengthThresh) {
									utterArray.add(curUtter);
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
			}
			
			//detectUtterance(overlap);
		}
		
		//handleSoundDetection();
		return true;
	}
	
	public void detectUtterance(int overlap) {
		//long curTime = System.currentTimeMillis();
		//utterArray.add(new SlidingWindow(curTime, ));
	}
	
	public void handleSoundDetection() {
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
			
			
			
			//windowIndex++;
			//System.out.println(windowValues/windowIndex + " " + (System.currentTimeMillis() - timeOfLastSound > windowSize));
			
			
			if(windowValues/windowArray.size() > threshold) {
				detectedUtterance();
			}
		
		} else if(silenceDetector.currentSPL() > threshold){
			detectedUtterance();
		}
		
	}
	
	public void detectedUtterance() {
		talkingAmount++;
		timeOfLastUtter = System.currentTimeMillis();
		System.out.println("Sound detected for " + user + ": " + (int)(silenceDetector.currentSPL()) + "dB SPL, Talking Amount: " + talkingAmount);
	}
	
	@Override
	public void processingFinished() {
		// TODO Auto-generated method stub
		
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
		int startTime = -1;
		int endTime = -1;
		ArrayList<Float> pcmData;
		int r, g, b;
		
		public Utterance(int startTime) {
			this.startTime = startTime;
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
		
		public void setEndTime(int endTime) {
			this.endTime = endTime;
		}
		
		public int getEndTime() {
			return endTime;
		}
		
		public int getStartTime() {
			return startTime;
		}
	}
	

}