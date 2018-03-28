package com.sgametrio.wsd;

import java.util.ArrayList;

import evaluation.InputInstance;
import evaluation.InputSentence;

public class SentenceRunner implements Runnable {
	private MyExecutor ex;
	private InputSentence input;
	
	public SentenceRunner (MyExecutor ex, InputSentence instance) {
		this.ex = ex;
		this.input = instance;
	}

	@Override
	public void run() {
		ex.performDisambiguation(input);
	}

}
