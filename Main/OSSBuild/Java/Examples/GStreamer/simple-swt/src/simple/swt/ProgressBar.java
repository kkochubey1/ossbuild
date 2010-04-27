
package simple.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * Creates a progressbar that allows switching b/t determinate and
 * indeterminate views.
 * 
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class ProgressBar extends Composite {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	private org.eclipse.swt.widgets.ProgressBar determinate;
	private org.eclipse.swt.widgets.ProgressBar indeterminate;
	private StackLayout layout;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public ProgressBar(Composite parent, int style) {
		super(parent, SWT.NULL);
		
		if ((style & SWT.VERTICAL) == 0)
			style |= SWT.HORIZONTAL;

		layout = new StackLayout();
		determinate = new org.eclipse.swt.widgets.ProgressBar(this, style);
		indeterminate = new org.eclipse.swt.widgets.ProgressBar(this, style | SWT.INDETERMINATE);
		setLayout(layout);
		setIndeterminate(false);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Methods">
	public boolean isIndeterminate() {
		return (layout.topControl == indeterminate);
	}

	public int getMinimum() {
		return ((org.eclipse.swt.widgets.ProgressBar)layout.topControl).getMinimum();
	}

	public int getMaximum() {
		return ((org.eclipse.swt.widgets.ProgressBar)layout.topControl).getMaximum();
	}

	public int getSelection() {
		return ((org.eclipse.swt.widgets.ProgressBar)layout.topControl).getSelection();
	}

	public void setMinimum(int value) {
		determinate.setMinimum(value);
		indeterminate.setMinimum(value);
	}

	public void setMaximum(int value) {
		determinate.setMaximum(value);
		indeterminate.setMaximum(value);
	}

	public void setSelection(int value) {
		determinate.setSelection(value);
		indeterminate.setSelection(value);
	}

	public void setIndeterminate(boolean value) {
		determinate.setVisible(!value);
		indeterminate.setVisible(value);
		layout.topControl = (value ? indeterminate : determinate);
		layout();
	}

	public void showError() {
		determinate.setState(SWT.ERROR);
	}

	public void showPaused() {
		determinate.setState(SWT.PAUSED);
	}

	public void showNormal() {
		determinate.setState(SWT.NORMAL);
	}
	//</editor-fold>
}
