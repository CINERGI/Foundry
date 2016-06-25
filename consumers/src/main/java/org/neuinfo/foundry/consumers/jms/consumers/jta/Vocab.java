package org.neuinfo.foundry.consumers.jms.consumers.jta;

import java.util.ArrayList;
import java.util.List;

public class Vocab {
	List<Concept> concepts;
	public Vocab(ArrayList<Concept> concepts)
	{
		this.concepts = concepts;
	}
}
