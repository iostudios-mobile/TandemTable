package TandemTable.sections.activities.headlines;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import vialab.simpleMultiTouch.events.TapEvent;
import vialab.simpleMultiTouch.events.VSwipeEvent;
import vialab.simpleMultiTouch.zones.RectZone;
import vialab.simpleMultiTouch.zones.TextZone;
import TandemTable.Colours;
import TandemTable.Languages;
import TandemTable.Sketch;
import TandemTable.sections.mainSection.MainSection;
import TandemTable.util.ArticleContainer;
import TandemTable.util.MiddleWord;

import com.memetix.mst.language.Language;

public class HeadlinesActivity {

	Sketch sketch;
	MainSection mainSection;

	int spaceX, topicIndex, index1 = 0, index2 = 0;
	int textSize;
	int height;
	int textYOffset;
	int widthOffset;
	int widthBody;
	int textYSpacing;
	int titleButtonW;
	// Colour picking
	public int red1 = 1, green1 = 1, blue1 = 1;
	public int red2 = 1, green2 = 1, blue2 = 1;
	
	int lastUser = 0;
	int colourPicker1;
	int colourPicker2;
	
	// Margins for the news articles
	int xMargin;
	int yMargin;
	
	float tweetTextSize;

	public HashMap<Integer, String> wordMap1 = new HashMap<Integer, String>();
	public HashMap<Integer, Float[]> wordX1 = new HashMap<Integer, Float[]>();

	public HashMap<Integer, String> wordMap2 = new HashMap<Integer, String>();
	public HashMap<Integer, Float[]> wordX2 = new HashMap<Integer, Float[]>();

	Language langTranslate1, langTranslate2;

	String topic1, topic2, lang1, lang2, back1, back2, topicExpanded1, topicExpanded2;
	String[] response;
	

	JSONArray results1, results2;

	boolean tapFlag1 = false;
	boolean tapFlag2 = false;
	boolean errorFlag1 = false;
	boolean errorFlag2 = false;
	boolean canceled = false;
	boolean wordTapped1 = false;
	boolean wordTapped2 = false;
	// Enable "Back To Headlines" buttons
	boolean b2Headlines = false;

	ArticleContainer body1, body2;
	RectZone background1, background2, swipeBackground1, swipeBackground2;
    TextZone backButton1, backButton2;
	MiddleWord middleZone;
	
	final int NUM_HEADLINES = 3;
	final int MAX_HEADLINES = 100;
	final int MAX_CHARACTERS = 100;
	final String ORDER = "relevance";////"date";

	TextZone[] headlines1 = new TextZone[NUM_HEADLINES];
	String[] titles1 = new String[NUM_HEADLINES];

	TextZone[] headlines2 = new TextZone[NUM_HEADLINES];
	String[] titles2 = new String[NUM_HEADLINES];

	HeadlineGetter hg;

	public HeadlinesActivity(Sketch sketch, MainSection mainSection, int topicIndex, String lang1, String lang2) {
		this.sketch = sketch;
		this.mainSection = mainSection;
		spaceX = sketch.getWidth() - sketch.lineX;
		this.topicIndex = topicIndex;
		this.lang1 = lang1;
		this.lang2 = lang2;
		textSize = sketch.getHeight()/50;
		height = 4*textSize;
		textYOffset = sketch.getHeight()/15;
		widthOffset = (int) (0.20*(sketch.getWidth()-sketch.lineX));
		tweetTextSize = sketch.getHeight()/40;
		widthBody = (int) (sketch.getWidth()- sketch.lineX - 2*widthOffset);
		textYSpacing = (int) (textSize*1.2);
		titleButtonW = (int) (sketch.getHeight()/200);

		xMargin = sketch.getWidth()/60;
		yMargin = sketch.getWidth()/60;

		if(lang1.equalsIgnoreCase("English")){
			back1 = Languages.back2HeadsE;
			topic1 = Languages.topicsE[topicIndex];
			langTranslate1 = Language.ENGLISH;

			setTopicsExpandedOrder(1, Languages.topicsExpandedE);
		} else if(lang1.equalsIgnoreCase("French")){
			back1 = Languages.back2HeadsF;
			topic1 = Languages.topicsF[topicIndex];
			langTranslate1 = Language.FRENCH;

			setTopicsExpandedOrder(1, Languages.topicsExpandedF);
			//topicExpanded1 = topic1;
		} else if(lang1.equalsIgnoreCase("Portuguese")){
			back1 = Languages.back2HeadsP;
			topic1 = Languages.topicsP[topicIndex];
			langTranslate1 = Language.PORTUGUESE;

			setTopicsExpandedOrder(1, Languages.topicsExpandedP);
			//topicExpanded1 = topic1;
		} else if(lang1.equalsIgnoreCase("Spanish")){
			back1 = Languages.back2HeadsS;
			topic1 = Languages.topicsS[topicIndex];
			langTranslate1 = Language.SPANISH;

			setTopicsExpandedOrder(1, Languages.topicsExpandedS);
			//topicExpanded1 = topic1;
		}

		if(lang2.equalsIgnoreCase("English")){
			back2 = Languages.back2HeadsE;
			topic2 = Languages.topicsE[topicIndex];
			langTranslate2 = Language.ENGLISH;

			setTopicsExpandedOrder(2, Languages.topicsExpandedE);

		} else if(lang2.equalsIgnoreCase("French")){
			back2 = Languages.back2HeadsF;
			topic2 = Languages.topicsF[topicIndex];
			langTranslate2 = Language.FRENCH;

			topicExpanded2 = "";

			setTopicsExpandedOrder(2, Languages.topicsExpandedF);
			//topicExpanded2 = topic2;			
		} else if(lang2.equalsIgnoreCase("Portuguese")){
			back2 = Languages.back2HeadsP;
			topic2 = Languages.topicsP[topicIndex];
			langTranslate2 = Language.PORTUGUESE;

			setTopicsExpandedOrder(2, Languages.topicsExpandedP);
			//topicExpanded2 = topic2;
		} else if(lang2.equalsIgnoreCase("Spanish")){
			back2 = Languages.back2HeadsS;
			topic2 = Languages.topicsS[topicIndex];
			langTranslate2 = Language.SPANISH;

			setTopicsExpandedOrder(2, Languages.topicsExpandedS);
			//topicExpanded2 = topic2;
		}

		
		mainSection.contentPrompt1.setText(sketch.learner1.newsPrompts[0]);
		mainSection.contentPrompt2.setText(sketch.learner2.newsPrompts[0]);
		
		createBackButtons();
		createBackground4Swipe();
		createHeadlineZones1();
		createHeadlineZones2();
		hg = new HeadlineGetter(sketch, this);
		hg.setPriority(Thread.MAX_PRIORITY);
		hg.start();
	}
	
	public void setTopicsExpandedOrder(int user, String[][] strList) {
		// Feedzilla does not support special characters
		Pattern nonSpecial = Pattern.compile("[^A-Za-z]");
		
		if(user == 1) {
			topicExpanded1 = "";
			String[] scrambled = sketch.scrambleStrings(strList[topicIndex]);

			for(String s: scrambled){
				if(nonSpecial.matcher(s).find()) {
					continue;
				}
				
				topicExpanded1 += s + " OR ";
			}

			topicExpanded1 = topicExpanded1.substring(0, topicExpanded1.length() - 4);
			//topicExpanded1 = topicExpanded1.replaceAll(" ", "");
			
			try {
				topicExpanded1 = URLEncoder.encode(topicExpanded1, "UTF-8").replaceAll("\\+", "%20");
			} catch (UnsupportedEncodingException e) {
				System.out.println("Encoding query string failed.");
				e.printStackTrace();
			}
		} else if(user == 2) {
			topicExpanded2 = "";

			String[] scrambled = sketch.scrambleStrings(strList[topicIndex]);

			for(String s: scrambled){
				if(nonSpecial.matcher(s).find()) {
					continue;
				}
				
				topicExpanded2 += s + " OR ";
			}

			topicExpanded2 = topicExpanded2.substring(0, topicExpanded2.length() - 4);
			//topicExpanded2 = topicExpanded2.replaceAll(" ", "");
			
			try {
				topicExpanded2 = URLEncoder.encode(topicExpanded2, "UTF-8").replaceAll("\\+", "%20");;
			} catch (UnsupportedEncodingException e) {
				System.out.println("Encoding query string failed.");
				e.printStackTrace();
			}
		}
	}

	public void createBackground4Swipe(){
		float halfH = sketch.getHeight()/2 - sketch.strokeW;
		float y1 = sketch.getHeight()/2 + sketch.strokeW;
		float width = sketch.getWidth() - (sketch.lineX + sketch.strokeW);

		swipeBackground1 = new RectZone(sketch.lineX + sketch.strokeW, y1, width, halfH, sketch.radius){
			public void vSwipeEvent(VSwipeEvent e) {

				if (!tapFlag1 && !errorFlag1){
					if(index1 >= MAX_HEADLINES - NUM_HEADLINES - (MAX_HEADLINES % NUM_HEADLINES) || index1 >= results1.length()){

						if(e.getSwipeType() == 1){
							index1 = 0; 
						} else if(index1 == 0){
							index1 = NUM_HEADLINES;
						} else {
							index1 -= NUM_HEADLINES;
						}
					} else {
						if(e.getSwipeType() == 1){
							index1 += NUM_HEADLINES;
						} else if(index1 == 0){
							index1 = NUM_HEADLINES;
						} else {
							index1 -= NUM_HEADLINES;
						}
					}
					setHeadlines(1);
					e.setHandled(tappableHandled);
				}
			}
		};
		swipeBackground1.setDrawBorder(false);
		swipeBackground1.setGestureEnabled("VSWIPE", true);
		swipeBackground1.setVSwipeDist(sketch.tSwipeThreshold);
		swipeBackground1.setColour(Colours.backgroundColour);
		sketch.client.addZone(swipeBackground1);


		swipeBackground2 = new RectZone(sketch.lineX + sketch.strokeW, 0, width, halfH, sketch.radius){
			public void vSwipeEvent(VSwipeEvent e) {
				if (!tapFlag2 && !errorFlag2){
					if(index2 >= MAX_HEADLINES - NUM_HEADLINES - (MAX_HEADLINES % NUM_HEADLINES) || index2 >= results2.length()){
						if(e.getSwipeType() == -1){
							index2 = 0; 
						} else if(index2 == 0){
							index2 = NUM_HEADLINES;
						} else {
							index2 -= NUM_HEADLINES;
						}
					} else {
						if(e.getSwipeType() == -1){
							index2 += NUM_HEADLINES;
						} else if(index2 == 0){
							index2 = NUM_HEADLINES;
						} else {
							index2 -= NUM_HEADLINES;
						}
					}
					setHeadlines(2);
					e.setHandled(tappableHandled);
				}

			}
		};
		swipeBackground2.setDrawBorder(false);
		swipeBackground2.setGestureEnabled("VSWIPE", true);
		swipeBackground2.setVSwipeDist(sketch.tSwipeThreshold);
		swipeBackground2.setColour(Colours.backgroundColour);
		sketch.client.addZone(swipeBackground2);
	}


	public void setHeadlines(int user){
		try {
			if(user == 1 && !errorFlag1){

				for(int i = 0; i < NUM_HEADLINES; i++){
					if((i+index1) < results1.length()){
						JSONObject article = (JSONObject)results1.get(i+index1);
						titles1[i] = (String) article.get("title");
						titles1[i] = titles1[i].trim();
						if(titles1[i].length() > MAX_CHARACTERS){
							titles1[i] = titles1[i].substring(0, MAX_CHARACTERS) + "...";
						}
						headlines1[i].setText(titles1[i]);
						setWidthTitle(titles1[i],  headlines1[i], 1);
					} else {
						String error = "No More Results";
						headlines1[i].setText(error);
						setWidthTitle(error,  headlines1[i], 1);
						headlines1[i].setGestureEnabled("Tap", false);

					}
				}

			} else if (user == 2 && !errorFlag2){

				for(int i = 0; i < NUM_HEADLINES; i++){
					if((i+index2) < results2.length()){
						JSONObject article = (JSONObject)results2.get(i+index2);
						titles2[i] = (String) article.get("title");
						titles2[i] = titles2[i].trim();
						if(titles2[i].length() > MAX_CHARACTERS){
							titles2[i] = titles2[i].substring(0, MAX_CHARACTERS) + "...";
						}
						headlines2[i].setText(titles2[i]);
						setWidthTitle(titles2[i],  headlines2[i], 2);
					} else {
						String error = "No More Results";
						headlines2[i].setText(error);
						setWidthTitle(error,  headlines2[i], 2);
						headlines2[i].setGestureEnabled("Tap", false);

					}

				}
			}
		} catch (JSONException e) {
			PApplet.println ("There was an error parsing the JSONObject.");
			e.printStackTrace();
		}
	}

	public void setWidthTitle(String str, TextZone tZone, int user){
		int middleSpace = sketch.getWidth()/20;
		sketch.textFont(Colours.pFont, textSize);

		if(user == 1){
			tZone.setX((int) (sketch.lineX + (sketch.getWidth()-sketch.lineX)/2 - sketch.textWidth(str)/2 - middleSpace));
		} else {
			tZone.setX((int) ((sketch.getWidth()-sketch.lineX)/2 - sketch.textWidth(str)/2 - middleSpace));
		}
		tZone.setWidth((int) (sketch.textWidth(str) + middleSpace*2));
	}

	public void setBackButton(TextZone tZone, int user, boolean activated){
		if(b2Headlines) {
			if(activated){
				if(user == 1){
					backButton1.setGestureEnabled("Tap", true);
					backButton1.setColour(Colours.unselectedZone);
					backButton1.setTextColour(Colours.zoneText.getRed(),Colours. zoneText.getGreen(), Colours.zoneText.getBlue());
	
				} else {
					backButton2.setGestureEnabled("Tap", true);
					backButton2.setColour(Colours.unselectedZone);
					backButton2.setTextColour(Colours.zoneText.getRed(),Colours. zoneText.getGreen(), Colours.zoneText.getBlue());
	
				}
			} else {
				if(user == 1){
					backButton1.setGestureEnabled("Tap", false);
					backButton1.setColour(Colours.fadedOutZone);
					backButton1.setTextColour(Colours.fadedText.getRed(),Colours. fadedText.getGreen(), Colours.fadedText.getBlue());
	
				} else {
					backButton2.setGestureEnabled("Tap", false);
					backButton2.setColour(Colours.fadedOutZone);
					backButton2.setTextColour(Colours.fadedText.getRed(),Colours. fadedText.getGreen(), Colours.fadedText.getBlue());
	
				}
			}
		}
	}

	public void createBackButtons(){
		if(b2Headlines) {
			// Back Button for user 1		
			backButton1 = new TextZone(mainSection.buttonX, mainSection.buttonYb2, sketch.buttonWidth, sketch.buttonHeight, sketch.radius, Colours.pFont, back1, sketch.textSize, "CENTER", "CENTER"){
				public void tapEvent(TapEvent e){
					if (isTappable()){
						sketch.client.removeZone(body1);
						setBackButton(this, 1, false);
						activateHeadlines(1);
						tapFlag1 = false;
						wordTapped1 = false;
						//if(imgFlag){
						//	sketch.client.removeZone(imgZone1);
						//}
						e.setHandled(tappableHandled);
					}
				}
			};
	
			backButton1.setTextColour(Colours.fadedText.getRed(), Colours.fadedText.getGreen(), Colours.fadedText.getBlue());
			backButton1.setColour(Colours.fadedOutZone.getRed(), Colours.fadedOutZone.getGreen(), Colours.fadedOutZone.getBlue());
			backButton1.setGestureEnabled("TAP", true, true);
			backButton1.setDrawBorder(false);
			sketch.client.addZone(backButton1);
	
			// Back Button for user 2	
			backButton2 = new TextZone(mainSection.buttonX, mainSection.buttonYt2, sketch.buttonWidth, sketch.buttonHeight, sketch.radius, Colours.pFont, back2, sketch.textSize, "CENTER", "CENTER"){
	
				public void tapEvent(TapEvent e){
					if (isTappable()){
						sketch.client.removeZone(body2);
						setBackButton(this, 2, false);
						activateHeadlines(2);
						tapFlag2 = false;
						wordTapped2 = false;
						//if(imgFlag){
						//	sketch.client.removeZone(imgZone1);
						//}
						e.setHandled(tappableHandled);
					}
				}
			};
	
			backButton2.rotate((float) (Colours.PI));
			backButton2.setTextColour(Colours.fadedText.getRed(), Colours.fadedText.getGreen(), Colours.fadedText.getBlue());
			backButton2.setColour(Colours.fadedOutZone.getRed(), Colours.fadedOutZone.getGreen(), Colours.fadedOutZone.getBlue());
			backButton2.setGestureEnabled("TAP", true, true);
			backButton2.setDrawBorder(false);
			sketch.client.addZone(backButton2);
		}
	}

	public void createHeadlineZones1(){
		final int y1 = sketch.getHeight()/2 + textYOffset/2;

		for(int i = 0; i < NUM_HEADLINES; i++){

			final int ii = i;
			headlines1[i] = new TextZone(sketch.lineX + widthOffset/4, (int) (y1 + (i+1.13)*textYOffset + (i*height)), widthBody, height, sketch.radius, Colours.pFont, "", textSize, "CENTER", "CENTER"){

				public void tapEvent(TapEvent e){
					if (isTappable() && !tapFlag1 && !errorFlag1){
						tapFlag1 = true;
						inactivateHeadlines(1);
						setBackButton(this, 1, true);

						try {
							JSONObject article = (JSONObject)results1.get(ii + index1);
							
							// Get body text content
							String body = new String((String) article.get("summary")).trim();
							final String bodyStr1 = body + "...";
							
							// Get author
							String author1 = "";
							if(article.has("author")){
								author1 = (String) article.get("author");
							}
							final String author = author1.trim();

							// Get date
							String date = "";
							if(article.has("publish_date")){
								date = (String) article.get("publish_date");
							}
							final String dateFinal = date.trim();
							
							
							// Set article container measurements
							final int bodyStartY = (int) (sketch.getHeight()/2 + 1.75*textYOffset);
							final int bodyHeight = sketch.getHeight() - bodyStartY;
							final int contentHeight = sketch.calculateTextHeight(bodyStr1, widthBody - xMargin*2, textSize, PConstants.LEFT, PConstants.TOP); 

							
							int graphicsHeightTemp = yMargin + 6*textYSpacing + contentHeight; 
							int temp= 0;

							if(graphicsHeightTemp  < bodyHeight){
								temp = bodyHeight;
							} else {
								temp = graphicsHeightTemp;
							}

							final int graphicsHeight = temp;

							PGraphics pg = sketch.createGraphics(widthBody, graphicsHeight, PConstants.P3D);
							final PGraphics backBuffer = sketch.createGraphics(widthBody, graphicsHeight, PConstants.P3D);
							
							
							background1 = new RectZone(sketch.lineX + sketch.strokeW, sketch.getHeight()/2 + sketch.strokeW, sketch.getWidth() - (sketch.lineX + sketch.strokeW), (sketch.getHeight()/2 - sketch.strokeW)){
								public void tapEvent(TapEvent e){
									sketch.client.removeZone(body1);
									sketch.client.removeZone(this);
									setBackButton(backButton1, 1, false);
									activateHeadlines(1);
									wordTapped1 = false;
									tapFlag1 = false;
									e.setHandled(true);
								}
							};
							background1.setGestureEnabled("Tap", true);
							background1.setDrawBorder(false);
							background1.setColour(Colours.backgroundColour);
							sketch.client.addZone(background1);
							sketch.client.pullToTop(middleZone.middleZone);

							// Create new article container for display article text
							body1 = new ArticleContainer(sketch, sketch.lineX + widthOffset, bodyStartY, widthBody, bodyHeight, sketch.radius, pg, 
									textYSpacing, textSize, xMargin, yMargin, graphicsHeight){
								
								public void setUpGraphicsContext(){

									PGraphics pg = this.getPGraphics();
									pg.beginDraw();
									pg.background(Colours.bodyTextBackground);
									drawHighlight(pg);
									pg.textFont(Colours.pFont, textSize); 
									sketch.textFont(Colours.pFont, textSize); 

									pg.textAlign(PConstants.CENTER, PConstants.CENTER);
									pg.fill(Colours.newsTextColor.getRed(), Colours.newsTextColor.getGreen(),Colours.newsTextColor.getBlue());
									float tw = pg.textWidth(titles1[ii]);
									float offset = 0;
									if(tw >= widthBody - xMargin*2){
										pg.text(titles1[ii], xMargin, yMargin, widthBody - xMargin*2, textSize*3);
										offset = 2*textYSpacing;
									} else {
										pg.text(titles1[ii], xMargin, yMargin, widthBody - xMargin*2, textSize);
									}
									pg.text(author, 0, yMargin + textYSpacing + offset, widthBody, textSize);
									pg.text(dateFinal, 0, yMargin + 2*textYSpacing + offset, widthBody, textSize);
									pg.textAlign(PConstants.LEFT, PConstants.TOP);
									setBody(pg, bodyStr1, xMargin, yMargin + 6*textYSpacing + offset, widthBody - xMargin, contentHeight*2);
									pg.endDraw();

									backBuffer.beginDraw();
									backBuffer.background(0);
									backBuffer.textFont(Colours.pFont, textSize); 
									backBuffer.textAlign(PConstants.CENTER, PConstants.CENTER);
									drawBackBuffer(backBuffer, titles1[ii], PConstants.CENTER, PConstants.CENTER, 0, xMargin, widthBody - xMargin);
									drawBackBuffer(backBuffer, author, PConstants.CENTER, PConstants.CENTER, textYSpacing + offset, 0, widthBody - xMargin);
									drawBackBuffer(backBuffer, dateFinal, PConstants.CENTER, PConstants.CENTER, 2*textYSpacing + offset, 0, widthBody - xMargin);
									drawBackBuffer(backBuffer, bodyStr1, PConstants.LEFT, PConstants.TOP, 6*textYSpacing + offset, xMargin, widthBody - xMargin);
									backBuffer.endDraw();
									red1 = 1;
									green1 = 1;
									blue1 = 1;

								}

								public void drawHighlight(PGraphics pg){
									if(wordTapped1){
										if(wordMap1.containsKey(colourPicker1)){
											pg.noStroke();
											pg.fill(Colours.highlightTextColour.getRed(), Colours.highlightTextColour.getGreen(),Colours.highlightTextColour.getBlue());
											pg.rect(wordX1.get(colourPicker1)[0], wordX1.get(colourPicker1)[1], wordX1.get(colourPicker1)[2], wordX1.get(colourPicker1)[3]);
										}
									}
								}


								public void tapEvent(TapEvent e){
									colourPicker1 = sketch.color(backBuffer.get((int)(e.getX()-getX()), (int)(e.getY()-getY() + getYOffset())));

									if(wordMap1.containsKey(sketch.color(backBuffer.get((int)(e.getX()-getX()), (int)(e.getY()-getY() + getYOffset()))))){
										middleZone.setMiddleText(wordMap1.get(sketch.color(backBuffer.get((int)(e.getX()-getX()), (int)(e.getY()-getY() + getYOffset())))));
										lastUser = 1;
										middleZone.started = true;
										wordTapped1 = true;
									}
								}
							};


							body1.setHeadActivity(mainSection.headlinesAct, 1);
							body1.setDrawBorder(false);
							body1.setGestureEnabled("Drag", true);
							body1.setGestureEnabled("Tap", true);
							body1.setShadow(true);
							body1.setShadowColour(Colours.shadow.getRed(), Colours.shadow.getGreen(), Colours.shadow.getBlue());
							body1.setStroke(false);
							body1.setShadowX(-sketch.shadowOffset);
							body1.setShadowY(-sketch.shadowOffset);
							body1.setShadowW(2*sketch.shadowOffset);
							body1.setShadowH(2*sketch.shadowOffset);

							sketch.client.addZone(body1);


							/*if(imgFlag){

								PImage pImage = sketch.loadImage((String) article.get("small_image_url"));
								imgZone1 = new ImageZone(pImage, sketch.lineX + widthOffset/4, y1, sketch.getWidth()/15, sketch.getWidth()/15);//Integer.parseInt(((String)article.get("small_image_width"))), Integer.parseInt(((String)article.get("small_image_height"))));


								imgZone1.setBorder(true);
								imgZone1.setGestureEnabled("Drag", true);
								imgZone1.setGestureEnabled("Pinch", true);
								imgZone1.setGestureEnabled("Rotate", true);
								sketch.client.addZone(imgZone1);
							}*/
						} catch (JSONException exception) {
							PApplet.println ("There was an error parsing the JSONObject.");
						}


						e.setHandled(tappableHandled);
					}
				}


			};

			headlines1[i].setTextColour(Colours.zoneText.getRed(), Colours.zoneText.getGreen(), Colours.zoneText.getBlue());
			headlines1[i].setColour(Colours.unselectedZone.getRed(), Colours.unselectedZone.getGreen(), Colours.unselectedZone.getBlue());
			headlines1[i].setGestureEnabled("TAP", true, true);

			headlines1[i].setDrawBorder(false);


			headlines1[i].setShadowColour(Colours.newsTitleShadow.getRed(), Colours.newsTitleShadow.getGreen(), Colours.newsTitleShadow.getBlue());
			headlines1[i].setShadow(true);
			headlines1[i].setStroke(false);
			headlines1[i].setShadowX(-titleButtonW);
			headlines1[i].setShadowY(-titleButtonW);
			headlines1[i].setShadowW(2*titleButtonW);
			headlines1[i].setShadowH(2*titleButtonW);
			headlines1[i].setActive(false);
			sketch.client.addZone(headlines1[i]);
		}
	}

	public void createHeadlineZones2(){		
		final int y2 = sketch.getHeight()/2 - textYOffset/2;

		for(int i = 0; i < NUM_HEADLINES; i++){

			final int ii = i;
			headlines2[i] = new TextZone(sketch.lineX + widthOffset/2, (int) ((y2 - (i+1.13)*textYOffset)- height - (i*height)), widthBody, height, sketch.radius, Colours.pFont, "", textSize, "CENTER", "CENTER"){

				public void tapEvent(TapEvent e){

					if (isTappable() && !tapFlag2 && !errorFlag2){
						tapFlag2 = true;
						inactivateHeadlines(2);
						setBackButton(this, 2, true);

						try {
							JSONObject article = (JSONObject)results2.get(ii + index2);
							
							// Get text body content
							String textBody = new String((String) article.get("summary")).trim();
							final String bodyStr2 = textBody + "...";
							
							// Get author
							String author2 = "";
							if(article.has("author")){
								author2 = (String) article.get("author");
							}
							final String author = author2;

							// Get date
							String date = "";
							if(article.has("publish_date")){
								date = (String) article.get("publish_date");
							}
							final String dateFinal = date.trim();
							
							//final boolean imgFlag = article.has("small_image");

							// Set article container's measurements
							final int bodyHeight = (int) (sketch.getHeight()/2 - 1.75*textYOffset);
							final int contentHeight = sketch.calculateTextHeight(bodyStr2, widthBody - xMargin*2, textSize, PConstants.LEFT, PConstants.TOP); 

							
							int graphicsHeightTemp = yMargin + 6*textYSpacing + contentHeight; 
							int temp= 0;

							if(graphicsHeightTemp  < bodyHeight){
								temp = bodyHeight;
							} else {
								temp = graphicsHeightTemp;
							}

							final int graphicsHeight = temp;

							PGraphics pg = sketch.createGraphics(widthBody, graphicsHeight, PConstants.P3D);
							final PGraphics backBuffer = sketch.createGraphics(widthBody, graphicsHeight, PConstants.P3D);

							background2 = new RectZone(sketch.lineX + sketch.strokeW, 0, sketch.getWidth() - (sketch.lineX + sketch.strokeW), sketch.getHeight()/2 - sketch.strokeW){
								public void tapEvent(TapEvent e){
									sketch.client.removeZone(body2);
									sketch.client.removeZone(this);
									setBackButton(backButton2, 2, false);
									activateHeadlines(2);
									tapFlag2 = false;
									wordTapped2 = false;
									e.setHandled(true);

								}
							};
							background2.setGestureEnabled("Tap", true);
							background2.setDrawBorder(false);
							background2.setColour(Colours.backgroundColour);
							sketch.client.addZone(background2);
							sketch.client.pullToTop(middleZone.middleZone);

							body2 = new ArticleContainer(sketch, sketch.lineX + widthOffset, 0, widthBody, bodyHeight, sketch.radius, pg, 
									textYSpacing, textSize, xMargin, yMargin, graphicsHeight){
								
								public void setUpGraphicsContext(){
									PGraphics pg = this.getPGraphics();
									pg.beginDraw();
									pg.background(Colours.bodyTextBackground);
									pg.textFont(Colours.pFont, textSize); 
									sketch.textFont(Colours.pFont, textSize); 
									drawHighlight(pg);
									pg.textAlign(PConstants.CENTER, PConstants.CENTER);
									pg.fill(Colours.newsTextColor.getRed(), Colours.newsTextColor.getGreen(),Colours.newsTextColor.getBlue());
									
									
									float tw = pg.textWidth(titles2[ii]);
									float offset = 0;
									
									if(tw >= widthBody - xMargin*2){
										pg.text(titles2[ii], xMargin, yMargin, widthBody - xMargin*2, textSize*3);
										offset = 2*textYSpacing;
									} else {
										pg.text(titles2[ii],  xMargin, yMargin, widthBody - xMargin*2, textSize);
									}
									
									pg.text(author, 0, yMargin + textYSpacing+ offset, widthBody, textSize);
									pg.text(dateFinal, 0, yMargin + 2*textYSpacing+ offset, widthBody, textSize);
									pg.textAlign(PConstants.LEFT, PConstants.TOP);
									setBody(pg, bodyStr2, xMargin, 0 + yMargin + 6*textYSpacing + offset, widthBody - xMargin, contentHeight*2);


									pg.endDraw();

									backBuffer.beginDraw();
									backBuffer.background(0);
									backBuffer.textFont(Colours.pFont, textSize); 
									backBuffer.textAlign(PConstants.CENTER, PConstants.CENTER);
									drawBackBuffer(backBuffer, titles2[ii], PConstants.CENTER, PConstants.CENTER, 0, xMargin, widthBody - xMargin);
									drawBackBuffer(backBuffer, author, PConstants.CENTER, PConstants.CENTER, textYSpacing + offset, 0, widthBody - xMargin);
									drawBackBuffer(backBuffer, dateFinal, PConstants.CENTER, PConstants.CENTER, 2*textYSpacing + offset, 0, widthBody - xMargin);
									drawBackBuffer(backBuffer, bodyStr2, PConstants.LEFT, PConstants.TOP, 6*textYSpacing + offset, xMargin, widthBody - xMargin);
									backBuffer.endDraw();
									//setPGraphics(backBuffer);
									red2 = 1;
									green2 = 1;
									blue2 = 1;
								}

								public void drawHighlight(PGraphics pg){
									if(wordTapped2){
										if(wordMap2.containsKey(colourPicker2)){
											pg.noStroke();
											pg.fill(Colours.highlightTextColour.getRed(), Colours.highlightTextColour.getGreen(),Colours.highlightTextColour.getBlue());
											pg.rect(wordX2.get(colourPicker2)[0], wordX2.get(colourPicker2)[1], wordX2.get(colourPicker2)[2], wordX2.get(colourPicker2)[3]);
										}
									}
								}

								public void tapEvent(TapEvent e){
									float newX =  ((e.getX() * this.matrix.m00 + e.getY() * this.matrix.m01 + 0 * this.matrix.m02 + this.matrix.m03))-x;
									float newY =  ((e.getX() * this.matrix.m10 + e.getY() * this.matrix.m11 + 0 * this.matrix.m12 + this.matrix.m13))-y;

									colourPicker2 = sketch.color(backBuffer.get((int)newX, (int)(newY + getYOffset())));
									if(wordMap2.containsKey(colourPicker2)){
										middleZone.setMiddleText(wordMap2.get(colourPicker2));
										lastUser = 2;
										middleZone.started = true;
										wordTapped2 = true;
										
									}
								}

							};

							body2.setHeadActivity(mainSection.headlinesAct, 2);
							body2.rotate((float) Colours.PI);
							body2.setDrawBorder(false);
							body2.setGestureEnabled("Drag", true);
							body2.setGestureEnabled("Tap", true);
							body2.setShadow(true);
							body2.setShadowColour(Colours.shadow.getRed(), Colours.shadow.getGreen(), Colours.shadow.getBlue());
							body2.setStroke(false);
							body2.setShadowX(-sketch.shadowOffset);
							body2.setShadowY(-sketch.shadowOffset);
							body2.setShadowW(2*sketch.shadowOffset);
							body2.setShadowH(2*sketch.shadowOffset);

							sketch.client.addZone(body2);

							/*if(imgFlag){

								int size = sketch.getWidth()/15;
								PImage pImage = sketch.loadImage((String) article.get("small_image_url"));
								imgZone2 = new ImageZone(pImage, sketch.lineX + widthOffset/4, y2-size, size, size);//Integer.parseInt(((String)article.get("small_image_width"))), Integer.parseInt(((String)article.get("small_image_height"))));


								imgZone2.rotate((float) Colours.PI);
								imgZone2.setBorder(true);
								imgZone2.setGestureEnabled("Drag", true);
								imgZone2.setGestureEnabled("Pinch", true);
								imgZone2.setGestureEnabled("Rotate", true);
								sketch.client.addZone(imgZone2);
							}*/
						} catch (JSONException exception) {
							PApplet.println ("There was an error parsing the JSONObject.");
						}

						e.setHandled(tappableHandled);
					}
				}

				
			};

			headlines2[i].setTextColour(Colours.zoneText.getRed(), Colours.zoneText.getGreen(), Colours.zoneText.getBlue());
			headlines2[i].setColour(Colours.unselectedZone.getRed(), Colours.unselectedZone.getGreen(), Colours.unselectedZone.getBlue());
			headlines2[i].rotate((float) Colours.PI);
			headlines2[i].setGestureEnabled("TAP", true, true);
			headlines2[i].setDrawBorder(false);

			headlines2[i].setShadowColour(Colours.newsTitleShadow.getRed(), Colours.newsTitleShadow.getGreen(), Colours.newsTitleShadow.getBlue());
			headlines2[i].setShadow(true);
			headlines2[i].setStroke(false);
			headlines2[i].setShadowX(-titleButtonW);
			headlines2[i].setShadowY(-titleButtonW);
			headlines2[i].setShadowW(2*titleButtonW);
			headlines2[i].setShadowH(2*titleButtonW);
			headlines2[i].setActive(false);
			sketch.client.addZone(headlines2[i]);

		}
	}

	public void createMiddleZone(){
		boolean sameLang = false;
		
		if(langTranslate1 == langTranslate2) {
			sameLang = true;
		}
		middleZone = new MiddleWord(sketch, hg, sameLang);
	}

	

	public void inactivateHeadlines(int user){
		if(user == 1){
			swipeBackground1.setActive(false);
		} else if(user == 2){
			swipeBackground2.setActive(false);
		}

		for(int i = 0; i < NUM_HEADLINES; i++){

			if(user == 1){
				headlines1[i].setActive(false);
			} else if (user == 2){
				headlines2[i].setActive(false);
			}

		}
	}

	public void activateHeadlines(int user){
		if(user == 1){
			swipeBackground1.setActive(true);
		} else if(user == 2){
			swipeBackground2.setActive(true);
		}
		
		for(int i = 0; i < NUM_HEADLINES; i++){

			if(user == 1){
				headlines1[i].setActive(true);
				headlines1[i].pullToTop();
			} else if (user == 2){
				headlines2[i].setActive(true);
				headlines2[i].pullToTop();
			}

		}
	}

	public void removeZones(){
		canceled = true;

		for(int i = 0; i < NUM_HEADLINES; i++){
			sketch.client.removeZone(headlines1[i]);
			sketch.client.removeZone(headlines2[i]);
		}
		
		if(middleZone != null) {
			sketch.client.removeZone(middleZone.middleZone);

			if(middleZone.fadedBack != null) {
				middleZone.removeTransButtons();
			}
		}

		sketch.client.removeZone(hg.loading);
		
		if(b2Headlines) {
			sketch.client.removeZone(backButton1);
			sketch.client.removeZone(backButton2);
		}
		//sketch.client.removeZone(imgZone1);
		sketch.client.removeZone(body1);
		sketch.client.removeZone(background1);

		//sketch.client.removeZone(imgZone2);
		sketch.client.removeZone(body2);
		sketch.client.removeZone(background2);

		sketch.client.removeZone(swipeBackground1);
		sketch.client.removeZone(swipeBackground2);
	}
	
	
}
