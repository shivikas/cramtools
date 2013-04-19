/*******************************************************************************
 * Copyright 2012 EMBL-EBI, Hinxton outstation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package net.sf.cram;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.text.MutableAttributeSet;

import net.sf.cram.encoding.read_features.Deletion;
import net.sf.cram.encoding.read_features.InsertBase;
import net.sf.cram.encoding.read_features.Insertion;
import net.sf.cram.encoding.read_features.ReadFeature;
import net.sf.cram.encoding.read_features.SoftClip;
import net.sf.cram.stats.MutableInt;

public class CramRecord implements Serializable {
	public static int MULTIFRAGMENT_FLAG = 0x1;
	public static int PROPER_PAIR_FLAG = 0x2;
	public static int SEGMENT_UNMAPPED_FLAG = 0x4;
	public static int NEGATIVE_STRAND_FLAG = 0x8;
	public static int FIRST_SEGMENT_FLAG = 0x10;
	public static int LAST_SEGMENT_FLAG = 0x20;
	public static int SECONDARY_ALIGNMENT_FLAG = 0x40;
	public static int VENDOR_FILTERED_FLAG = 0x80;
	public static int DUPLICATE_FLAG = 0x100;

	public ReadTag[] tags;

	public int index = 0;
	private int alignmentStart;
	public int alignmentStartOffsetFromPreviousRecord;

	private int readLength;

	public int recordsToNextFragment = -1;

	private byte[] readBases;
	private byte[] qualityScores;

	private List<ReadFeature> readFeatures;

	private int readGroupID = 0;
	private int flags ;

	// pointers to the previous and next segments in the template:
	public CramRecord next, previous;

	// mate flags:
	private Byte mateFlags = null;
	public boolean mateUmapped = false;
	public boolean mateNegativeStrand = false;

	// compression flags:
	private Byte compressionFlags = null;
	public boolean hasMateDownStream = false;
	public boolean detached = false;
	public boolean forcePreserveQualityScores = false;

	public int mateSequnceID = -1;
	public int mateAlignmentStart = 0;

	private int mappingQuality;

	private String sequenceName;
	public int sequenceId;
	private String readName;
	public int templateSize;
	public long counter = 1;

	public byte[] tagIds;
	public MutableInt tagIdsIndex;

	public int getFlags() {
		return flags;
	}

	public void setFlags(int value) {
		this.flags = value;
	}

	public byte getMateFlags() {
		if (mateFlags == null) {
			byte b = 0;
			b |= mateUmapped ? 1 : 0;
			b <<= 1;
			b |= mateNegativeStrand ? 1 : 0;
			mateFlags = new Byte(b);
		}
		return mateFlags;
	}

	public void setMateFlags(byte value) {
		int b = 0xFF & value;

		mateNegativeStrand = ((b & 1) == 0) ? false : true;
		b >>>= 1;
		mateUmapped = ((b & 1) == 0) ? false : true;

		mateFlags = value;
	}

	public void resetMateFlags() {
		mateFlags = null;
	}

	public byte getCompressionFlags() {
		if (compressionFlags == null) {
			byte b = 0;
			b |= hasMateDownStream ? 1 : 0;
			b <<= 1;
			b |= detached ? 1 : 0;
			b <<= 1;
			b |= forcePreserveQualityScores ? 1 : 0;
			compressionFlags = new Byte(b);
		}
		return compressionFlags;
	}

	public void setCompressionFlags(byte value) {
		int b = 0xFF & value;

		forcePreserveQualityScores = ((b & 1) == 0) ? false : true;
		b >>>= 1;
		detached = ((b & 1) == 0) ? false : true;
		b >>>= 1;
		hasMateDownStream = ((b & 1) == 0) ? false : true;

		compressionFlags = value;
	}

	public void resetCompressionFlags() {
		compressionFlags = null;
	}

	public int getAlignmentStart() {
		return alignmentStart;
	}

	public void setAlignmentStart(int alignmentStart) {
		this.alignmentStart = alignmentStart;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof CramRecord))
			return false;

		CramRecord r = (CramRecord) obj;

		if (alignmentStart != r.alignmentStart)
			return false;
		if (isNegativeStrand() != r.isNegativeStrand())
			return false;
		if (isVendorFiltered() != r.isVendorFiltered())
			return false;
		if (isSegmentUnmapped() != r.isSegmentUnmapped())
			return false;
		if (readLength != r.readLength)
			return false;
		if (isLastSegment() != r.isLastSegment())
			return false;
		if (recordsToNextFragment != r.recordsToNextFragment)
			return false;
		if (isFirstSegment() != r.isFirstSegment())
			return false;
		if (mappingQuality != r.mappingQuality)
			return false;

		if (!deepEquals(readFeatures, r.readFeatures))
			return false;

		if (!Arrays.equals(readBases, r.readBases))
			return false;
		if (!Arrays.equals(qualityScores, r.qualityScores))
			return false;

		if (!areEqual(flags, r.flags))
			return false;

		if (!areEqual(readName, r.readName))
			return false;

		return true;
	}

	private boolean areEqual(Object o1, Object o2) {
		if (o1 == null && o2 == null)
			return true;
		return o1 != null && o1.equals(o2);
	}

	private boolean deepEquals(Collection<?> c1, Collection<?> c2) {
		if ((c1 == null || c1.isEmpty()) && (c2 == null || c2.isEmpty()))
			return true;
		if (c1 != null)
			return c1.equals(c2);
		return false;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("[");
		if (readName != null)
			sb.append(readName).append("; ");
		sb.append("flags=").append(getFlags());
		sb.append("; aloffset=").append(alignmentStartOffsetFromPreviousRecord);
		sb.append("; mateoffset=").append(recordsToNextFragment);
		sb.append("; mappingQuality=").append(mappingQuality);

		if (readFeatures != null)
			for (ReadFeature feature : readFeatures)
				sb.append("; ").append(feature.toString());

		if (readBases != null)
			sb.append("; ").append("bases: ").append(new String(readBases));
		if (qualityScores != null)
			sb.append("; ").append("qscores: ")
					.append(new String(qualityScores));

		sb.append("]");
		return sb.toString();
	}

	public int getReadLength() {
		return readLength;
	}

	public void setReadLength(int readLength) {
		this.readLength = readLength;
	}

	public boolean isLastFragment() {
		return isLastSegment();
	}

	public void setLastFragment(boolean lastFragment) {
		this.setLastSegment(lastFragment);
	}

	public int getRecordsToNextFragment() {
		return recordsToNextFragment;
	}

	public void setRecordsToNextFragment(int recordsToNextFragment) {
		this.recordsToNextFragment = recordsToNextFragment;
	}

	public boolean isReadMapped() {
		return isSegmentUnmapped();
	}

	public void setReadMapped(boolean readMapped) {
		this.setSegmentUnmapped(readMapped);
	}

	public List<ReadFeature> getReadFeatures() {
		return readFeatures;
	}

	public void setReadFeatures(List<ReadFeature> readFeatures) {
		this.readFeatures = readFeatures;
	}

	public byte[] getReadBases() {
		return readBases;
	}

	public void setReadBases(byte[] readBases) {
		this.readBases = readBases;
	}

	public byte[] getQualityScores() {
		return qualityScores;
	}

	public void setQualityScores(byte[] qualityScores) {
		this.qualityScores = qualityScores;
	}

	public int getReadGroupID() {
		return readGroupID;
	}

	public void setReadGroupID(int readGroupID) {
		this.readGroupID = readGroupID;
	}

	public int getMappingQuality() {
		return mappingQuality;
	}
	
	public void setMappingQuality(int mappingQuality) {
		this.mappingQuality = mappingQuality;
	}

	public String getSequenceName() {
		return sequenceName;
	}

	public void setSequenceName(String sequenceName) {
		this.sequenceName = sequenceName;
	}

	public String getReadName() {
		return readName;
	}

	public void setReadName(String readName) {
		this.readName = readName;
	}

	public int calcualteAlignmentEnd() {
		if (readFeatures == null || readFeatures.isEmpty())
			return alignmentStart + readLength;

		int len = readLength;
		for (ReadFeature f : readFeatures) {
			switch (f.getOperator()) {
			case InsertBase.operator:
				len--;
				break;
			case Insertion.operator:
				len -= ((Insertion) f).getSequence().length;
				break;
			case SoftClip.operator:
				len -= ((SoftClip) f).getSequence().length;
				break;
			case Deletion.operator:
				len += ((Deletion) f).getLength();
				break;

			default:
				break;
			}
		}
		return alignmentStart + len;
	}

	public boolean isMultiFragment() {
		return (flags & MULTIFRAGMENT_FLAG) != 0;
	}

	public void setMultiFragment(boolean multiFragment) {
		flags = multiFragment ? flags | MULTIFRAGMENT_FLAG : flags & ~MULTIFRAGMENT_FLAG;
	}

	public boolean isSegmentUnmapped() {
		return (flags & SEGMENT_UNMAPPED_FLAG) != 0;
	}

	public void setSegmentUnmapped(boolean segmentUnmapped) {
		flags = segmentUnmapped ? flags | SEGMENT_UNMAPPED_FLAG : flags & ~SEGMENT_UNMAPPED_FLAG;
	}

	public boolean isFirstSegment() {
		return (flags & FIRST_SEGMENT_FLAG) != 0;
	}

	public void setFirstSegment(boolean firstSegment) {
		flags = firstSegment ? flags | FIRST_SEGMENT_FLAG : flags & ~FIRST_SEGMENT_FLAG;
	}

	public boolean isLastSegment() {
		return (flags & LAST_SEGMENT_FLAG) != 0;
	}

	public void setLastSegment(boolean lastSegment) {
		flags = lastSegment ? flags | LAST_SEGMENT_FLAG : flags & ~LAST_SEGMENT_FLAG;
	}

	public boolean isSecondaryALignment() {
		return (flags & SECONDARY_ALIGNMENT_FLAG) != 0;
	}

	public void setSecondaryALignment(boolean secondaryALignment) {
		flags = secondaryALignment ? flags | SECONDARY_ALIGNMENT_FLAG : flags & ~SECONDARY_ALIGNMENT_FLAG;
	}

	public boolean isVendorFiltered() {
		return (flags & VENDOR_FILTERED_FLAG) != 0;
	}

	public void setVendorFiltered(boolean vendorFiltered) {
		flags = vendorFiltered ? flags | VENDOR_FILTERED_FLAG : flags & ~VENDOR_FILTERED_FLAG;
	}
	
	public boolean isProperPair() {
		return (flags & PROPER_PAIR_FLAG) != 0;
	}

	public void setProperPair(boolean properPair) {
		flags = properPair ? flags | PROPER_PAIR_FLAG : flags & ~PROPER_PAIR_FLAG;
	}

	public boolean isDuplicate() {
		return (flags & DUPLICATE_FLAG) != 0;
	}

	public void setDuplicate(boolean duplicate) {
		flags = duplicate ? flags | DUPLICATE_FLAG : flags & ~DUPLICATE_FLAG;
	}
	
	public boolean isNegativeStrand() {
		return (flags & NEGATIVE_STRAND_FLAG) != 0;
	}

	public void setNegativeStrand(boolean negativeStrand) {
		flags = negativeStrand ? flags | NEGATIVE_STRAND_FLAG : flags & ~NEGATIVE_STRAND_FLAG;
	}

}
