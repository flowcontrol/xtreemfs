// Copyright (c) 2010 Minor Gordon
// All rights reserved
// 
// This source file is part of the XtreemFS project.
// It is licensed under the New BSD license:
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
// * Neither the name of the XtreemFS project nor the
// names of its contributors may be used to endorse or promote products
// derived from this software without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL Minor Gordon BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


#include "open_file_table.h"
using namespace xtreemfs;


class OpenFileTable::Entry
{
public:
  Entry( uint32_t reader_count, uint32_t writer_count )
    : reader_count( reader_count ), writer_count( writer_count )
  { }

  uint32_t get_reader_count() const { return reader_count; }
  uint32_t get_writer_count() const { return writer_count; }
  void inc_reader_count() { ++reader_count; }
  void inc_writer_count() { ++writer_count; }
  uint32_t dec_reader_count() { return --reader_count; }
  uint32_t dec_writer_count() { return --writer_count; }

private:
  uint32_t reader_count, writer_count;
};


OpenFileTable::~OpenFileTable()
{
  for
  (
    map<Path, Entry*>::iterator entry_i = entries.begin();
    entry_i != entries.end();
    ++entry_i
  )
    delete entry_i->second;
}

void OpenFileTable::open( const Path& path, bool writer )
{
}

void OpenFileTable::release( const Path& path, bool writer )
{
}