/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2017-2020  Michael Sabin
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import jpsxdec.adpcm.SpuAdpcmDecoder;
import jpsxdec.i18n.I;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.sharedaudio.DecodedAudioPacket;
import jpsxdec.util.Fraction;


public class SquareAudioSectorPairToAudioPacket implements SquareAudioSectorToSquareAudioSectorPair.Listener {

    @Nonnull
    private final SpuAdpcmDecoder.Stereo _decoder;
    private final ByteArrayOutputStream _buffer = new ByteArrayOutputStream();
    @CheckForNull
    private DecodedAudioPacket.Listener _listener;

    public SquareAudioSectorPairToAudioPacket(double dblVolume) {
        _decoder = new SpuAdpcmDecoder.Stereo(dblVolume);
    }
    public void setListener(@CheckForNull DecodedAudioPacket.Listener listener) {
        _listener = listener;
    }

    @Override
    public void pairDone(@Nonnull SquareAudioSectorPair pair, @Nonnull ILocalizedLogger log)
            throws LoggedFailure
    {
        _buffer.reset();
        try {
            long lngSamplesWritten = _decoder.getSampleFramesWritten();
            pair.decode(_decoder, _buffer);
            if (_decoder.hadCorruption())
                log.log(Level.WARNING, I.SPU_ADPCM_CORRUPTED(pair.getStartSector(), lngSamplesWritten));
        } catch (IOException ex) {
            throw new RuntimeException("Should never happen", ex);
        }
        if (_listener != null) {
            AudioFormat af = _decoder.getOutputFormat(pair.getSampleFramesPerSecond());
            DecodedAudioPacket packet = new DecodedAudioPacket(-1, af,
                    new Fraction(pair.getPresentationSector()), _buffer.toByteArray());
            _listener.audioPacketComplete(packet, log);
        }
    }

    @Override
    public void endOfSectors(@Nonnull ILocalizedLogger log) {
        // there's no work-in-progress to send to listener
    }

    public double getVolume() {
        return _decoder.getVolume();
    }

    public @Nonnull AudioFormat getFormat(int iSampleFramesPerSecond) {
        return _decoder.getOutputFormat(iSampleFramesPerSecond);
    }

}
