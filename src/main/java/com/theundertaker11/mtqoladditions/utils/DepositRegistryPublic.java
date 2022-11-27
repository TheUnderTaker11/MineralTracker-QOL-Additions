/**
 * Modified version of lordfokas.mineraltrakcer.tracker.DepositRegistry.
 * 
 * He made all his methods protected, and this is only client-side reading anyways, so might as well re-make the class with 
 * public methods instead.
 * 
 */

package com.theundertaker11.mtqoladditions.utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

import lordfokas.mineraltracker.plugins.terrafirma.TFCClusterizer;
import lordfokas.mineraltracker.tracker.ClientTracker;
import lordfokas.mineraltracker.tracker.IDeposit;
import lordfokas.mineraltracker.tracker.IMapProxy;
import lordfokas.mineraltracker.tracker.Serializer;
import lordfokas.mineraltracker.tracker.ServerTracker;

public abstract class DepositRegistryPublic
{
    private final File file;
    private final File backup;
    public final TreeSet<IDeposit> deposits;
    
    public DepositRegistryPublic(final File file) {
        this.file = file;
        this.backup = new File(file.getAbsolutePath() + ".bak");
        this.deposits = new TreeSet<IDeposit>();
        System.err.printf("New Registry [%s] @ %s\n", this.getClass().getSimpleName(), file.getAbsolutePath());
    }
    
    public void save() throws Exception {
        if (this.file.exists()) {
            this.file.renameTo(this.backup);
        }
        else {
            this.file.getParentFile().mkdirs();
        }
        this.file.createNewFile();
        try (final DataOutputStream stream = new DataOutputStream(new FileOutputStream(this.file))) {
            this.saveContents(stream);
        }
        if (this.backup.exists()) {
            this.backup.delete();
        }
    }
    
    private void saveContents(final DataOutputStream stream) throws Exception {
        stream.writeInt(this.deposits.size());
        for (final IDeposit d : this.deposits) {
            Serializer.write(stream, d);
        }
    }
    
    public void load() {
        try {
            if (!this.file.exists()) {
                return;
            }
            try (final DataInputStream stream = new DataInputStream(new FileInputStream(this.file))) {
                this.loadContents(stream);
            }
        }
        catch (Exception e) {
        	e.printStackTrace();
        	System.out.println("-----------------------------------------------------------------------");
            throw new RuntimeException(e);
        }
    }
    
    private void loadContents(final DataInputStream stream) throws Exception {
        for (int size = stream.readInt(), i = 0; i < size; ++i) {
            final IDeposit deposit = Serializer.read(stream);
            this.deposits.add(deposit);
            this.onNewDeposit(deposit);
        }
    }
    
    public boolean has(final IDeposit deposit) {
        return this.deposits.contains(deposit);
    }
    
    public void add(final IDeposit deposit) {
        if (this.has(deposit)) {
            final IDeposit existing = this.deposits.subSet(deposit, true, deposit, true).first();
            if (existing.getTimestamp() < deposit.getTimestamp()) {
                this.deposits.remove(existing);
                this.addAndSave(deposit);
            }
        }
        else {
            this.addAndSave(deposit);
        }
    }
    
    private void addAndSave(final IDeposit deposit) {
        this.deposits.add(deposit);
        try {
            this.save();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        this.onNewDeposit(deposit);
    }
    
    public abstract void onNewDeposit(final IDeposit p0);
    
    public static class Client extends DepositRegistryPublic
    {
        private TFCClusterizer clusterizer;
        private final File csv;
        
        public Client(final File file) {
            super(file);
            this.clusterizer = new TFCClusterizer();
            this.csv = new File(file.getAbsolutePath() + ".csv");
        }
        
        @Override
        public void onNewDeposit(final IDeposit deposit) {
            this.clusterizer.clusterize(deposit);
            //ClientTracker.INSTANCE.getMap().addNewDeposit(deposit);
        }
        
        public void onSnapshot(final List<IDeposit> deposits) {
            this.deposits.clear();
            this.deposits.addAll(deposits);
            final IMapProxy map = ClientTracker.INSTANCE.getMap();
            map.clearAll();
            this.clusterizer = new TFCClusterizer();
            for (final IDeposit deposit : deposits) {
                this.clusterizer.clusterize(deposit);
                map.addNewDeposit(deposit);
            }
        }
        
        @Override
        public void save() throws Exception {
            super.save();
        }
    }
}
