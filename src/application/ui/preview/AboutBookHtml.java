package application.ui.preview;

import java.text.MessageFormat;

import org.daisy.braille.utils.pef.PEFBook;

import com.googlecode.ajui.AContainer;
import com.googlecode.ajui.ADefinitionDescription;
import com.googlecode.ajui.ADefinitionList;
import com.googlecode.ajui.ADefinitionTerm;
import com.googlecode.ajui.AHeading;
import com.googlecode.ajui.ALabel;
import com.googlecode.ajui.APage;
import com.googlecode.ajui.AParagraph;

import application.l10n.Messages;

public class AboutBookHtml extends APage {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4158397890850715579L;

	public AboutBookHtml(PEFBook book) {
		AContainer content = new AContainer();
		Iterable<String> data = book.getTitle();
		if (data==null || !data.iterator().hasNext()) {

		} else {
			for (String s: data) {
				AHeading a = new AHeading(2);
				a.add(new ALabel(s));
				content.add(a);
			}
		}
		data = book.getAuthors();
		if (data==null || !data.iterator().hasNext()) {

		} else {
			StringBuilder sb = new StringBuilder();
			String delimiter = "";
			for (String s : data) {
				sb.append(delimiter + s);
				delimiter = ", ";
			}
			AParagraph p = new AParagraph();
			p.add(new ALabel(sb.toString()));
			content.add(p);
		}
		ADefinitionList dl = new ADefinitionList();
		{
			ADefinitionTerm dt = new ADefinitionTerm();
			dt.add(new ALabel(Messages.SIZE.localize()));
			dl.add(dt);
		}
		{	
			ADefinitionDescription dd = new ADefinitionDescription();
			dd.add(new ALabel(MessageFormat.format(Messages.SIZE_PAGES.localize(), book.getPages())));
			dl.add(dd);
		}
		{
			StringBuilder s = new StringBuilder();
			for (int i=1; i<=book.getVolumes(); i++) {
				if (i==1) {
					//s.append("(");
				}
				s.append(book.getSheets(i));
				if (i<book.getVolumes()) {
					s.append(" + ");
				} else {
					//s.append(")");
				}
			}
			{	
				ADefinitionDescription dd = new ADefinitionDescription();
				dd.add(new ALabel(MessageFormat.format(Messages.SIZE_SHEETS.localize(), book.getSheets(), s)));
				dl.add(dd);
			}
		}
		{	
			ADefinitionDescription dd = new ADefinitionDescription();
			dd.add(new ALabel(MessageFormat.format(Messages.SIZE_VOLUMES.localize(), book.getVolumes())));
			dl.add(dd);
		}
		{
			ADefinitionTerm dt = new ADefinitionTerm();
			dt.add(new ALabel(Messages.DIMENSIONS.localize()));
			dl.add(dt);
		}
		{	
			ADefinitionDescription dd = new ADefinitionDescription();
			dd.add(new ALabel(MessageFormat.format(Messages.FILE_DIMENSIONS.localize(), book.getMaxWidth(), book.getMaxHeight())));
			dl.add(dd);
		}
		{
			ADefinitionTerm dt = new ADefinitionTerm();
			dt.add(new ALabel(Messages.DUPLEX.localize()));
			dl.add(dt);
		}
		{	
			ADefinitionDescription dd = new ADefinitionDescription();
			float ratio = book.getPages()/(float)book.getPageTags();
			String info;
			if (ratio<=1) {
				info = Messages.DUPLEX_YES.localize();
			} else if (ratio>=2) {
				info = Messages.DUPLEX_NO.localize();
			} else {
				info = Messages.DUPLEX_MIXED.localize();
			}
			dd.add(new ALabel(info));
			dl.add(dd);
		}
		{
			ADefinitionTerm dt = new ADefinitionTerm();
			dt.add(new ALabel(Messages.EIGHT_DOT.localize()));
			dl.add(dt);
		}
		{	
			ADefinitionDescription dd = new ADefinitionDescription();
			dd.add(new ALabel((book.containsEightDot() ? Messages.YES.localize() : Messages.NO.localize())));
			dl.add(dd);
		}
		content.add(dl);
		dl = new ADefinitionList();
		for (String key : book.getMetadataKeys()) {
			{
				ADefinitionTerm dt = new ADefinitionTerm();
				dt.add(new ALabel(Messages.getString("Worker.dc."+key)));
				dl.add(dt);
			}
			for (String value : book.getMetadata(key)) {
				{	
					ADefinitionDescription dd = new ADefinitionDescription();
					dd.add(new ALabel(value));
					dl.add(dd);
				}
			}
		}
		content.add(dl);

		/*
    	AParagraph p = new AParagraph();
    	ALink a = new ALink("#");
    	a.addAttribute("onclick", "window.open('book.xml','source'); return false;");
    	a.add(new ALabel(Messages.getString(L10nKeys.XSLT_VIEW_SOURCE)));
    	p.add(a);
    	content.add(p);*/
		setView(content);
	}

}
