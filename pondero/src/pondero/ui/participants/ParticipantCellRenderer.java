package pondero.ui.participants;

import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import pondero.UiUtil;

@SuppressWarnings("serial")
public class ParticipantCellRenderer extends JLabel implements TableCellRenderer {

    public ParticipantCellRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        setHorizontalAlignment(column == 0 ? SwingConstants.TRAILING : SwingConstants.LEADING);
        setText(" " + value.toString() + "   ");
        if (isSelected) {
            setBackground(UiUtil.getListSelectedBackgroundColor());
            setForeground(UiUtil.getListSelectedForegroundColor());
        } else {
            if (row % 2 == 0) {
                setBackground(UiUtil.getListBackgroundEvenColor());
            } else {
                setBackground(UiUtil.getListBackgroundOddColor());
            }
            setForeground(UiUtil.getListForegroundColor());
        }
        return this;
    }

}
