/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
dojo.provide("Bundle");
dojo.require("org.apache.aries.samples.demo.prototype.Preferences");
dojo.require("org.apache.aries.samples.demo.prototype.BundleAppearance");
dojo.require("TwistieSection");
dojo.require("dojox.gfx");
dojo.require("dojox.gfx.fx");
dojo.require("dojox.gfx.Moveable");
dojo.require("dojo.data.ItemFileWriteStore");

dojo.declare("Bundle", null, {

	// properties
	id: null,
	name: null,
	state: null,	
	version: null,
	hidden: false,

	// gfx objects
	group: null,
	surface: null,
	outline: null,
	
	textName: null,
	nameRule: null,
	
	importTwistie: null,
	exportTwistie: null,
	
	textState: null,
	stateRule: null,
	
	textVersion: null,
	versionRule: null,
	
	// underlying datastore
	jsonStore: null,

	// configuration preferences
	preferences: null, 
	bundleAppearance: null,
	
	// dimensions/coords
	x: 0,
	y: 0,
	minWidth: 150,
	minHeight: 80,
	ruleOffset: 5,
	ruleHeight: 20,
	width: null,
	height: null,
	
	linesFrom: null,
	linesTo: null,	
	
	
	constructor: function(surface, id, name, state, version, jsonStore, preferences) {
	this.surface=surface;
	this.id=id;
	this.name=name;
	this.version=version;
	this.state=state;
	this.x=0;
	this.y=0;
	this.jsonStore=jsonStore;
	this.preferences=new org.apache.aries.samples.demo.prototype.Preferences();
	this.bundleAppearance=new org.apache.aries.samples.demo.prototype.BundleAppearance(state, this.preferences);
	
	this.height=100;
	this.width=this.minWidth;
		
	this.linesFrom = new Array();
	this.linesTo = new Array();
	
	this.calcWidthBasedOnContent();
	
	this.initGfx();
	
	var bundleThis = this;
	var importCallBack = function(){
	    var importCallback = function(data)
	    {
	      if (data != null && typeof data == 'object'){
		      bundleThis.importTwistie.items = data;	 
		      bundleThis.importTwistie.addItemsToDisplay();
		      bundleThis.resize();
	      }
	    };				
		ServerSideClass.getPackageImports(id, importCallback);
	};
	var exportCallBack = function(){
	    var exportCallback = function(data)
	    {
	      if (data != null && typeof data == 'object'){
		      bundleThis.exportTwistie.items = data;	 
		      bundleThis.exportTwistie.addItemsToDisplay();
		      bundleThis.resize();
	      }
	    };				
		ServerSideClass.getPackageExports(id, exportCallback);
	};
	
	// name, parentGroup, owningBundle, x, y
	var impy = (this.ruleHeight*2)+this.ruleOffset+25;
	var expy = impy+12;
	this.importTwistie=new TwistieSection("Imports", this.group, this, 5, impy, importCallBack, this.bundleAppearance);
	this.exportTwistie=new TwistieSection("Exports", this.group, this, 5, expy, exportCallBack, this.bundleAppearance);
	
	// update size to account for twisties.
	this.resize();
	var mover = new dojox.gfx.Moveable(this.group);
    dojo.connect(mover, "onMoved", this, "onMoved");  
    
    this.group.connect("onclick",dojo.hitch(this,"onClick"));
    this.group.connect("onmouseenter",dojo.hitch(this,"onMouseEnter"));
    this.group.connect("onmouseleave",dojo.hitch(this,"onMouseLeave"));
},
initGfx: function() {
	this.group = this.surface.createGroup();

	this.outline = this.group.createRect({x: 0, y: 0, width: this.width, height: this.height, r: 5});
	
	this.drawState(this.state);

	this.createNameRule(this.width);
	this.createStateRule(this.width);
	this.createVersionRule(this.width);

	this.createNameText(this.name);
	this.createStateText(this.state, this.id);
	this.createVersionText(this.version);
        
},
createNameText: function(name){
	if (this.textName != null)
	{
		this.group.remove(this.textName);
	}
	var y = (this.ruleHeight*0.5)+this.ruleOffset+5;
	this.textName = this.group.createText({x: 5, y: y, text: name, align: "start"})
	.setFont({family: this.bundleAppearance.fontFamily, fontStretch: this.bundleAppearance.fontStretch, size: "12pt"})
	.setFill(this.bundleAppearance.textFill);	
},
createVersionText: function(version){
	if (this.textVersion != null)
	{
		this.group.remove(this.textVersion);
	}

	if (this.preferences.showVersion) {
		var y = (this.ruleHeight*1.5)+this.ruleOffset+5;
		this.textVersion = this.group.createText({x: 5, y: y, text: version, align: "start"})
		.setFont({family: this.bundleAppearance.fontFamily, fontStretch: this.bundleAppearance.fontStretch, size: "12pt"})
		.setFill(this.bundleAppearance.textFill);
	}
},
drawState: function(state){
	this.outline.setStroke({width: 2, color: this.bundleAppearance.lineColour, style: this.bundleAppearance.lineStyle});
	this.outline.setFill({type: "linear",  x1: 0, y1: 0, x2: 150, y2: 80,
			colors: [{ offset: 0, color: this.bundleAppearance.backgroundColour },{ offset: 1, color: "white" } ]});

},
createStateText: function(state, id){
	if (this.textState != null)
	{
		this.group.remove(this.textState);
	}
	if (this.preferences.showState)
	{
		var stateidstr = "" + id + ":" + state;
		var multiplier=2.5; 
		// We need to draw things slightly higher if we didn't have a line for the version
		if (!this.preferences.showVersion)
		{
			multiplier = 1.5;
		}
		var y = (this.ruleHeight*multiplier)+this.ruleOffset+5;
		this.textState = this.group.createText({x: 5, y: y, text: stateidstr, align: "start"})
		.setFont({family: this.bundleAppearance.fontFamily, fontStretch: this.bundleAppearance.fontStretch, size: "12pt"})
		.setFill(this.bundleAppearance.textFill);	
	}
},
createNameRule: function(width){
	// Clean up the previous line
	if (this.versionRule != null)
	{
		this.group.remove(this.nameRule);
	}
	var y = (this.ruleHeight+this.ruleOffset);
	this.nameRule = this.group.createLine({x1:0, y1: y, x2: width, y2: y})
	.setStroke({width:1, color: '#808080'});
},
createVersionRule: function(width){
	// Clean up the previous line
	if (this.versionRule != null)
	{
		this.group.remove(this.versionRule);
	}
	if (this.preferences.showVersion)
	{
		var y = (this.ruleHeight*2)+this.ruleOffset;
		this.versionRule = this.group.createLine({x1:0, y1:y, x2: width, y2: y})
		.setStroke({width:1, color: '#808080'});
	}
},
createStateRule: function(width){
	// Clean up the previous line
	if (this.stateRule != null)
	{
		this.group.remove(this.stateRule);
	}
	// Only add a rule if we're going to be adding a line for the state
	if (this.preferences.showState)
	{
		// We need to draw things slightly higher if we didn't have a line for the version
		var multiplier=3;
		if (!this.preferences.showVersion)
		{
			multiplier = 2;
		}
		var y = (this.ruleHeight*multiplier)+this.ruleOffset;
		this.stateRule = this.group.createLine({x1:0, y1:y, x2: width, y2: y})
		.setStroke({width:1, color: '#808080'});
	} 	
},
resize: function(){
	var width = this.width;
	if(width < this.importTwistie.width ){
		width = this.importTwistie.width;
	}
	if(width < this.exportTwistie.width ){
		width = this.exportTwistie.width;
	}
	
	var height = this.height;
	if( this.importTwistie.height > 0 || this.exportTwistie.height > 0 ){
		height = height + this.importTwistie.height;
		height = height + this.exportTwistie.height;	
	}
	
	var multiplier=2;
	if (!this.preferences.showState)
	{
		multiplier=multiplier - 1; 
	}
	if (!this.preferences.showVersion)
	{
		multiplier=multiplier - 1; 
	}
	height = height - this.ruleHeight*(2 - multiplier);
	
	var impy = ((this.ruleHeight)*multiplier)+this.ruleOffset+25;
	var expy = impy+12+this.importTwistie.height;
	// make the twisties appear in the correct location.
	this.importTwistie.updatePosition(5,(impy));
	this.exportTwistie.updatePosition(5,(expy));
	
	this.createNameRule(width);
	this.createStateRule(width);
	this.createVersionRule(width);
	this.outline.setShape({x: 0, y: 0, width: width, height: height, r: 5});
	this.group.moveToFront();
	
	dojo.forEach(this.linesTo, function(line){ line.updateLine() });
	dojo.forEach(this.linesFrom, function(line){ line.updateLine() });
},
calcWidthBasedOnContent: function(){
	// adjust the width, based on the name length
	if(this.name.length > 18){
		var extra = this.name.length - 18;
		extra = extra * 6;
		var proposedNewWidth = this.minWidth + extra;
		if(proposedNewWidth > this.width){
			this.width = proposedNewWidth;
		}	
	}
	// adjust the width, based on the version length
	if(this.version.length > 18){
		var extra = this.name.length - 18;
		extra = extra * 6;
		var proposedNewWidth = this.minWidth + extra;	
		if(proposedNewWidth > this.width){
			this.width = proposedNewWidth;
		}	
	}	
},
update: function(id, name, state, version) {	
	if(this.name != name || this.state != state || this.version != version){
		this.name = name;		
		this.state = state;
		this.version = version;
		
		this.bundleAppearance.update(state);
		this.calcWidthBasedOnContent();
		
		var _this=this;
	    this.jsonStore.fetch({query: { id: this.id }, onComplete: function(item){
	        _this.jsonStore.setValue(item[0], "name", _this.name);
	        _this.jsonStore.setValue(item[0], "state", _this.state);
	    }});	
	
	    this.drawState(state);
		this.createNameText(this.name);

		this.createStateText(this.state, this.id);
		
		this.createVersionText(this.version);
		
		this.importTwistie.update();
		this.exportTwistie.update();
	}
},
moveToNewPlace: function(x, y) {
	this.group.setTransform({dx:x, dy:y});
	this.x = x;
	this.y = y;
	this.updateAfterMove();	
},
toggleHidden: function(){
	var hideMe = !this.hidden;

	var _this=this;
    this.jsonStore.fetch({query: { id: this.id }, onComplete: function(item){
        _this.jsonStore.setValue(item[0], "hidden", hideMe);
    }});
	
	if(hideMe){
		// cheat.. move it off canvas..
		this.group.setTransform({dx:-1000, dy:-1000});	
	}else{
		// bring it back =) good job we remembered where it was supposed to go !
		this.group.setTransform({dx:this.x, dy:this.y});	
	}
		
	this.hidden = hideMe;
	
	// en/disable all lines to/from..
	dojo.forEach(this.linesTo, function(line){  line.updateVisibility(); line.updateLine() });
	dojo.forEach(this.linesFrom, function(line){ line.updateVisibility(); line.updateLine() });
},
onMoved: function(mover, shift) {   
    this.x=this.group.matrix.dx;
    this.y=this.group.matrix.dy;
    
	this.updateAfterMove();
},
updateAfterMove: function(){
	
	dojo.forEach(this.linesTo, function(line){ line.updateLine() });
	dojo.forEach(this.linesFrom, function(line){ line.updateLine() });
	
	var _this=this;
    this.jsonStore.fetch({query: { id: this.id }, onComplete: function(item){
        _this.jsonStore.setValue(item[0], "x", _this.x);
        _this.jsonStore.setValue(item[0], "y", _this.y);
    }});
},
onClick: function() {
	this.group.moveToFront();
	dojox.gfx.fx.animateStroke({
	    shape: this.outline,
	    duration: 500,
	    color: {start: "#6F0000", end: "#FF3030"},
	    width: {start:10, end: 2},
	    join:  {values: ["miter", "bevel", "round"]},
	    style: this.lineStyle
	}).play();
},
onMouseEnter: function() {
	this.outline.setStroke({width: 3, color: '#FF3030', style: this.lineStyle});
},
onMouseLeave: function() {
	// this.outline.setStroke({width: 2, color: '#808080'});
	dojox.gfx.fx.animateStroke({
	    shape: this.outline,
	    duration: 500,
	    color: {start: "#FF3030", end: this.bundleAppearance.lineColour},
	    width: {end: 2},
	    join:  {values: ["miter", "bevel", "round"]},
	    style: this.lineStyle
	}).play();
}

});