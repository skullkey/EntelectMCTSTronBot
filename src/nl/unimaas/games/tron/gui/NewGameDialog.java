package nl.unimaas.games.tron.gui;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstraints;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import nl.unimaas.games.tron.player.Player;

public class NewGameDialog extends JDialog {
	private static final long serialVersionUID = 6254080952918617918L;
	private int maxPlayers = 2;
	private boolean result = false;
	private JTextField[] nameFields;
	private JComboBox[] typeBoxes;
	
	public NewGameDialog(Dialog owner) {
		super(owner, "New game", true);
		initComponents();
	}
	
	public NewGameDialog(Frame owner) {
		super(owner, "New game", true);
		initComponents();
	}
	
	private void initComponents() {
		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		Border border = new EmptyBorder(10, 10, 0, 10);
		
		//north
		JLabel infoLabel = new JLabel("Select the players");
		infoLabel.setBorder(border);
		contentPane.add(infoLabel, BorderLayout.NORTH);
		contentPane.add(new JLabel("teststs"), BorderLayout.NORTH);
		
		
		//center
		JPanel centerPanel = new JPanel();
		centerPanel.setBorder(border);
		TableLayout centerLayout = new TableLayout(new double[][] {
				{TableLayout.PREFERRED, 106, TableLayout.FILL},
				{9, TableLayout.PREFERRED, TableLayout.PREFERRED}});
		centerPanel.setLayout(centerLayout);
		centerLayout.setHGap(5);
		centerLayout.setVGap(5);
		//generate rows
		ArrayList<JTextField> nameFields = new ArrayList<JTextField>();
		ArrayList<JComboBox> typeBoxes = new ArrayList<JComboBox>();
		for (int i = 1; i < maxPlayers + 1; i++) {
			JComboBox typeBox = new JComboBox(Player.types);
			typeBox.setSelectedIndex(Math.min(i, typeBox.getItemCount()) - 1);
			JTextField nameField = new JTextField("Player" + i);
			nameFields.add(nameField);
			typeBoxes.add(typeBox);
			centerPanel.add(new JLabel("Player " + i), new TableLayoutConstraints(0, i, 0, i, TableLayoutConstraints.RIGHT, TableLayoutConstraints.FULL));
			centerPanel.add(typeBox, new TableLayoutConstraints(1, i, 1, i, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));
			centerPanel.add(nameField, new TableLayoutConstraints(2, i, 2, i, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));
		}
		this.nameFields = nameFields.toArray(new JTextField[nameFields.size()]);
		this.typeBoxes = typeBoxes.toArray(new JComboBox[typeBoxes.size()]);
		contentPane.add(centerPanel, BorderLayout.CENTER);
		
		//south
		JPanel southPanel = new JPanel();
		southPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		final JButton playButton = new JButton("Play");
		playButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				result = true;
				setVisible(false);
			}
		});
		final JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				result = false;
				setVisible(false);
			}
		});
		southPanel.add(playButton);
		southPanel.add(cancelButton);
		contentPane.add(southPanel, BorderLayout.SOUTH);
		
		setSize(320, 180);
		setResizable(false);
		setLocationRelativeTo(getOwner());
		getRootPane().setDefaultButton(playButton);
	}
	
	public boolean showDialog() {
		setVisible(true);
		return result;
	}
	
	public void setMaxPlayerCount(int count) {
		if (count < 2)
			throw new IllegalArgumentException();
		maxPlayers = count;
	}
	
	public int getMaxPlayerCount() {
		return maxPlayers;
	}
	
	public String getPlayerType(int index) {
		return (String) typeBoxes[index].getSelectedItem();
	}
	
	public String getPlayerName(int index) {
		return nameFields[index].getText();
	}
}
