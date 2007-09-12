HistoryGui { 

	classvar <w, <>docTitle = "History repeats", <>docHeight=120;
	
	var <history, <w, <startBut, <pop, <keyPop, <topBut;
	var <doc, <oldDocs, <docFlag = \sameDoc, <>stickMode=0; 
	
	var <filters, <filteredIndices, <filteredShorts, <filtering = false;
	var <filtBut, <keyPop, <filTextV, <textV, <filterBtn;
	var lastLineSelected = 0, lastLinesShown;
	 
	*new { |history, where, numTextLines=12| 
		^super.new.init(history, where ? (0@0), numTextLines); 
	}
	
	init { |inHist, where, numTextLines=12| 
		var sys = GUI.scheme, bounds;
		var closebut, listV, font, flow;
		bounds = where @ (where + (300@400));
		
		font = sys.font.new("Osaka", 9);				////
		w = sys.window.new("History", bounds).front;	////
		flow = w.view.decorator = FlowLayout(w.bounds.moveTo(0,0), 2@2, 2@2);
		history = inHist; 
		
		filters = [\all, ""]; 
		
		textV = sys.textView.new(w, Rect(0,0,300, numTextLines * 12)).string_("")
			.enterInterpretsSelection_(false)
			.keyDownAction_({ |txvw, char, mod, uni, keycode| 
				char.postcs;
				if ([3, 13].includes(char.ascii)) {
					"gets here?".postln;
					this.findDoc; doc.string_(textV.string);
				};
			})
			.resize_(2);

			// to do: disable if history is not current!
		startBut = sys.button.new(w, Rect(0, 0, 50, 20)) ////
			.states_([ ["start"], ["end"]])
			.action_({ |btn|
				switch(btn.value, 
					0, { if (history == History.current) { History.end } }, 
					1, { if (history == History.current) { History.start } }
				);
			});
		
		filtBut = sys.button.new(w, Rect(42, 22, 32, 20)) ////
			.states_([["all"], ["filt"]]).action_({ |btn| 
				this.filtering_(btn.value > 0);
				if (filtering) { this.filterLines };
				 history.hasMovedOn = true;
			});
		
		keyPop = sys.popUpMenu.new(w, Rect(64, 22, 40, 20))
			.items_([\all] ++ history.keys).value_(0)
			.action_({ |pop| this.setKeyFilter(pop.items[pop.value]) });
		
		filTextV = sys.textView.new(w, Rect(106,22,100,20)).string_("")
			.enterInterpretsSelection_(false)
			.resize_(2)
			.keyDownAction_({ |txvw, char, mod, uni, keycode| 
				this.setStrFilter(txvw.string);
				if (this.filtering) { this.filterLines; }
			});
		topBut = sys.button.new(w, Rect(190, 22, 32, 20))
			.states_([["top"], ["keep"]]).value_(0)
			.resize_(3)
			.action_({ |but| this.stickMode_(but.value) });
		
		sys.button.new(w, Rect(42, 22, 32, 20)) ////
			.states_([["rip"]])
			.resize_(3)
			.action_({ |btn| this.findDoc; doc.string_(textV.string); });		
		listV = sys.listView.new(w,bounds.moveTo(2, 44).resizeBy(-4, -48))
			.font_(font)
			.items_([])
			.resize_(5)
			.background_(Color.grey(0.62))
			.action_({ |lview| 
				var index = lview.value;
				if (lview.items.isEmpty) { 
					"no entries yet.".postln; 
				} {
					lastLineSelected = listV.items[index]; 
					if (filtering.not) { 
						this.postInlined(index) 
					} {
						this.postInlined(filteredIndices[index]) 
					}
				}
			})
			.enterKeyAction_({ |lview|
				var index = lview.value;
				if (filtering) { index = filteredIndices[index] };
				try { 
					history.lines[index][2].postln.interpret.postln; 
				//	"did execute.".postln; 
				} { 
					"execute line from history failed.".postln;
				};
			});
		history.hasMovedOn = true;
		
		SkipJack({ 
			var newIndex, selectedLine, linesToShow, keys; 
			var newStr = filTextV.string; 
			if (filTextV.hasFocus and: (newStr != filters[1])) {
				this.setStrFilter(newStr);
			}; // clumsy, but filTextV has no usable action...
			
			if (history.hasMovedOn) { 
				startBut.enabled_(history.isCurrent); 
				startBut.value_(History.started.binaryValue).refresh;
				
				filtBut.value_(filtering.binaryValue).refresh;
				if (filTextV.hasFocus.not) { filTextV.string_(filters[1]) };
				keys = [\all] ++ history.keys.asArray.sort;
				keyPop.items_(keys);
				keyPop.value_(keys.indexOf(filters[0]) ? 0);
				if (stickMode == 1) { 
						// remember old selection
					selectedLine = (lastLinesShown ? [])[listV.value]; 
				} { }; 
				
				linesToShow = if (filtering.not) { 
					history.lineShorts.array.copy
				} { 
					this.filterLines; 
					filteredShorts;
				};
			//	[\linesToShow, linesToShow].postln; 
				
				if (linesToShow != lastLinesShown) { 
				//	"or updating listview here?".postln; 
					listV.items_(linesToShow); 
					lastLinesShown = linesToShow;
				}; 
				newIndex = if (selectedLine.isNil) { 0 }
					{ linesToShow.indexOf(selectedLine) };
				listV.value_(newIndex ? 0);
				if(stickMode == 0) { listV.action.value(listV) };
				history.hasMovedOn = false;
			};
		}, 1, { w.isClosed }, "histwin");
	}
	setKeyFilter { |key| filters.put(0, key); this.filterLines; }
	setStrFilter { |str| filters.put(1, str); this.filterLines; }

	filtering_ { |flag=true| 	
		 filtering = flag;
		 history.hasMovedOn_(true); 
	}
	filterOn { this.filtering_(true) }
	filterOff { this.filtering_(false) }
	
	filterLines { 
		filteredIndices = history.indicesFor(*filters);
		filteredShorts = history.lineShorts[filteredIndices]; 
		defer { 
			keyPop.value_(keyPop.items.indexOf(filters[0] ? 0)); 
			filTextV.string_(filters[1]);
		};
		if (filtering) { history.hasMovedOn = true; };
	}
	postInlined { |index|
		var line;
		if (history.lines.isNil) { "no history lines yet.".postln; ^this }; 
		line = history.lines[index]; 	
		if (line.isNil) { "history: no line found!".inform; ^this };
		textV.string_(line[2]);
	}
	postDoc { |index| 
		var line;
		if (history.lines.isNil) { "no history lines yet.".postln; ^this }; 
		line = history.lines[index]; 	
		if (line.isNil) { "history: no line found!".inform; ^this };
		this.findDoc; 
		doc.string_(line[2]).front;
		try { this.alignDoc };
		w.front;
	}
	alignDoc { 
		var docbounds, winbounds;
		docbounds = doc.bounds;
		winbounds = w.bounds;
		doc.bounds_(
			Rect(
				winbounds.left, 
				winbounds.top + winbounds.height + 24, 
				winbounds.width,
				docHeight
			)
		)
	}
	findDoc { 
		if (docFlag == \newDoc) { oldDocs = oldDocs.add(doc) }; 
		if (docFlag == \newDoc or: doc.isNil or: { Document.allDocuments.includes(doc).not }) { 
			doc = Document(docTitle).bounds_(Rect(300, 500, 300, 100));
		};	
		oldDocs = oldDocs.select {|d| d.notNil and: { d.dataptr.notNil } };
	} 
}