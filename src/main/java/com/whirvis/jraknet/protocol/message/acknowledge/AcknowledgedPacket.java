/*
 *       _   _____            _      _   _          _   
 *      | | |  __ \          | |    | \ | |        | |  
 *      | | | |__) |   __ _  | | __ |  \| |   ___  | |_ 
 *  _   | | |  _  /   / _` | | |/ / | . ` |  / _ \ | __|
 * | |__| | | | \ \  | (_| | |   <  | |\  | |  __/ | |_ 
 *  \____/  |_|  \_\  \__,_| |_|\_\ |_| \_|  \___|  \__|
 *                                                  
 * the MIT License (MIT)
 *
 * Copyright (c) 2016-2019 Trent "Whirvis" Summerlin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * the above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.  
 */
package com.whirvis.jraknet.protocol.message.acknowledge;

import java.util.ArrayList;
import java.util.Arrays;

import com.whirvis.jraknet.Packet;
import com.whirvis.jraknet.RakNetPacket;

/**
 * An <code>ACK</code> packet.
 * <p>
 * This packet is sent when a packet that requires an acknowledgement receipt is
 * received. This enables for servers and clients to know when the other side
 * has received their message, which can be crucial during the login process.
 * 
 * @author Trent "Whirvis" Summerlin
 * @since JRakNet v1.0.0
 * @see Record
 */
public class AcknowledgedPacket extends RakNetPacket {

	/**
	 * The records containing the sequence IDs.
	 */
	public ArrayList<Record> records;

	/**
	 * Creates an <code>ACK</code> packet to be encoded.
	 * 
	 * @param acknowledge
	 *            <code>true</code> if the records inside the packet are
	 *            acknowledged, <code>false</code> if the records are not
	 *            acknowledged.
	 * @see #encode()
	 */
	protected AcknowledgedPacket(boolean acknowledge) {
		super(acknowledge ? ID_ACK : ID_NACK);
		this.records = new ArrayList<Record>();
	}

	/**
	 * Creates an <code>ACK</code> packet to be encoded.
	 * 
	 * @see #encode()
	 */
	public AcknowledgedPacket() {
		this(true);
	}

	/**
	 * Creates an <code>ACK</code> packet to be decoded.
	 * 
	 * @param packet
	 *            the original packet whose data will be read from in the
	 *            {@link #decode()} method.
	 */
	public AcknowledgedPacket(Packet packet) {
		super(packet);
		this.records = new ArrayList<Record>();
	}

	/**
	 * Returns whether or not the records inside the packet are acknowledged.
	 * 
	 * @return <code>true</code> if the records inside the packet are
	 *         acknowledged, <code>false</code> if the records are not
	 *         acknowledged.
	 */
	public boolean isAcknowledgement() {
		return this.getId() == ID_ACK;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Before encoding, all records will be condensed. This means that all
	 * records that can be converted to ranged records will be converted to
	 * ranged records, making them use less memory.
	 */
	@Override
	public void encode() {
		/*
		 * Get sequence IDs and sort them in ascending order. This is crucial in
		 * order for condensing to occur.
		 */
		int[] sequenceIds = Record.getSequenceIds(records);
		Arrays.sort(sequenceIds);
		records.clear(); // Prevent duplicates

		// Condense records
		for (int i = 0; i < sequenceIds.length; i++) {
			int startIndex = sequenceIds[i];
			int endIndex = startIndex;
			if (i + 1 < sequenceIds.length) {
				while (endIndex + 1 == sequenceIds[i + 1] && i + 1 < sequenceIds.length) {
					endIndex = sequenceIds[++i]; // This value is sequential
				}
			}
			records.add(new Record(startIndex, endIndex == startIndex ? -1 : endIndex));
		}

		// Encode packet
		this.writeUnsignedShort(records.size());
		for (Record record : records) {
			this.writeUnsignedByte(record.isRanged() ? 0x00 : 0x01);
			this.writeTriadLE(record.getIndex());
			if (record.isRanged()) {
				this.writeTriadLE(record.getEndIndex());
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * After decoding is finished, all records will be simplified. This means
	 * that all ranged records will be converted to single records, making it
	 * easier to cycle through them.
	 */
	@Override
	public void decode() {
		// Decode packet
		int size = this.readUnsignedShort();
		for (int i = 0; i < size; i++) {
			boolean ranged = (this.readUnsignedByte() == 0x00);
			if (ranged == false) {
				records.add(new Record(this.readTriadLE()));
			} else {
				records.add(new Record(this.readTriadLE(), this.readTriadLE()));
			}
		}

		// Simplify records
		int[] sequenceIds = Record.getSequenceIds(records);
		records.clear(); // Prevent duplicates
		for (int i = 0; i < sequenceIds.length; i++) {
			records.add(new Record(sequenceIds[i]));
		}
	}

}
