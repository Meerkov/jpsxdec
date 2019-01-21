/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2019  Michael Sabin
 * All rights reserved.
 *
 * Redistribution and use of the jPSXdec code or any derivative works are
 * permitted provided that the following conditions are met:
 *
 *  * Redistributions may not be sold, nor may they be used in commercial
 *    or revenue-generating business activities.
 *
 *  * Redistributions that are modified from the original source must
 *    include the complete source code, including the source code for all
 *    components used by a binary built from the modified sources. However, as
 *    a special exception, the source code distributed need not include
 *    anything that is normally distributed (in either source or binary form)
 *    with the major components (compiler, kernel, and so on) of the operating
 *    system on which the executable runs, unless that component itself
 *    accompanies the executable.
 *
 *  * Redistributions must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or
 *    other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jpsxdec.modules.square;

import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdSectorXaSubHeader.SubMode;
import jpsxdec.i18n.exception.LocalizedIncompatibleException;
import jpsxdec.modules.strvideo.SectorStrVideo;
import jpsxdec.modules.video.sectorbased.SectorAbstractVideo;
import jpsxdec.modules.video.sectorbased.VideoSectorCommon16byteHeader;


/** Represents a Chrono Cross video sector. */
public class SectorChronoXVideo extends SectorAbstractVideo {
    
    // .. Static stuff .....................................................

    public static final long CHRONO_CROSS_VIDEO_CHUNK_MAGIC1 = 0x81010160L;
    public static final long CHRONO_CROSS_VIDEO_CHUNK_MAGIC2 = 0x01030160L;

    // .. Fields ..........................................................

    @Nonnull
    private final VideoSectorCommon16byteHeader _header;
    private int  _iWidth;               //  16   [2 bytes]
    private int  _iHeight;              //  18   [2 bytes]
    private int  _iRunLengthCodeCount;  //  20   [2 bytes]
    private int  _iQuantizationScale;   //  24   [2 bytes]
    private int  _iVersion;             //  26   [2 bytes]
    private long _lngFourZeros;         //  28   [4 bytes]
    //   32 TOTAL
    
    @Override
    public int getVideoSectorHeaderSize() { return 32; }

    public SectorChronoXVideo(@Nonnull CdSector cdSector) {
        super(cdSector);
        _header = new VideoSectorCommon16byteHeader(cdSector);
        if (isSuperInvalidElseReset()) return;

        // only if it has a sector header should we check if it reports DATA or VIDEO
        if (subModeMaskMatch(SubMode.MASK_DATA | SubMode.MASK_VIDEO, 0))
            return;

        if (_header.lngMagic != CHRONO_CROSS_VIDEO_CHUNK_MAGIC1 &&
            _header.lngMagic != CHRONO_CROSS_VIDEO_CHUNK_MAGIC2)
            return;

        if (!_header.isChunkNumberStandard()) return;
        if (!_header.isChunksInFrameStandard()) return;
        if (!_header.isFrameNumberStandard()) return;
        if (!_header.isUsedDemuxSizeStandard()) return;
        _iWidth = cdSector.readSInt16LE(16);
        if (_iWidth < 1) return;
        _iHeight = cdSector.readSInt16LE(18);
        if (_iHeight < 1) return;
        _iRunLengthCodeCount = cdSector.readUInt16LE(20);
        if (_iRunLengthCodeCount < 1) return;
        int iMagic3800 = cdSector.readUInt16LE(22);
        if (iMagic3800 != 0x3800) return;
        _iQuantizationScale = cdSector.readSInt16LE(24);
        if (_iQuantizationScale < 1) return;
        _iVersion = cdSector.readUInt16LE(26);
        _lngFourZeros = cdSector.readUInt32LE(28);

        setProbability(100);
    }

    // .. Public methods ...................................................

    public @Nonnull String getTypeName() {
        return "CX Video";
    }

    public String toString() {
        return String.format("%s %s frame:%d chunk:%d/%d %dx%d ver:%d " +
            "{demux frame size=%d rlc=%d qscale=%d 4*00=%08x}",
            getTypeName(),
            super.cdToString(),
            _header.iFrameNumber,
            _header.iChunkNumber,
            _header.iChunksInThisFrame,
            _iWidth,
            _iHeight,
            _iVersion,
            _header.iUsedDemuxedSize,
            _iRunLengthCodeCount,
            _iQuantizationScale,
            _lngFourZeros
            );
    }

    public int getChunkNumber() {
        return _header.iChunkNumber;
    }

    public int getChunksInFrame() {
        return _header.iChunksInThisFrame;
    }

    public int getHeaderFrameNumber() {
        return _header.iFrameNumber;
    }

    public int getWidth() {
        return _iWidth;
    }

    public int getHeight() {
        return _iHeight;
    }

    public void replaceVideoSectorHeader(@Nonnull byte[] abNewDemuxData, int iNewUsedSize,
                                         int iNewMdecCodeCount, @Nonnull byte[] abCurrentVidSectorHeader)
            throws LocalizedIncompatibleException
    {
        SectorStrVideo.replaceStrVideoSectorHeader(abNewDemuxData, iNewUsedSize,
                                                   iNewMdecCodeCount, abCurrentVidSectorHeader);
    }

}

